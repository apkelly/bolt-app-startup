package com.github.apkelly.bolt.startup

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.os.Bundle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class BoltAppInitializer(private val context: Context) {

    // The list of BoltInitializers in the AndroidManifest.xml file.
    private val manifestInitializers: MutableSet<Class<out BoltInitializer?>> = mutableSetOf()

    // A node in the dependency tree.
    private data class BoltTreeNode(
        val name: String,
        val dependencies: MutableList<BoltTreeNode> = mutableListOf()
    )

    // The root node of the dependency tree.
    private val boltInitializerTreeRoot = BoltTreeNode(name = "root")

    companion object {
        fun getInstance(context: Context): BoltAppInitializer {
            return BoltAppInitializer(context)
        }
    }

    /**
     * Fetch the AndroidManifest.xml meta-data and
     * start the initializatoin process.
     */
    fun discoverAndInitialize() {
        try {
            val provider = ComponentName(
                context.packageName,
                BoltInitializationProvider::class.java.name
            )
            val providerInfo: ProviderInfo = context.packageManager
                .getProviderInfo(provider, PackageManager.GET_META_DATA)
            val metadata = providerInfo.metaData
            discoverAndInitialize(metadata)
        } catch (exception: PackageManager.NameNotFoundException) {
            throw BoltException(exception)
        }
    }

    /**
     * Using the BoltInitializers in the AndroidManifest.xml as a
     * starting point, build a dependency tree and then initialise
     * the components asynchronously.
     */
    private fun discoverAndInitialize(metadata: Bundle?) {
        val startup = context.getString(R.string.bolt_startup)
        try {
            if (metadata != null) {
                val keys = metadata.keySet()
                for (key in keys) {
                    val value = metadata.getString(key, null)
                    if (startup == value) {
                        val clazz = Class.forName(key)
                        if (BoltInitializer::class.java.isAssignableFrom(clazz)) {
                            val component = clazz as Class<out BoltInitializer>
                            manifestInitializers.add(component)
                        }
                    }
                }

                // Using the entries in the manifest, build a tree of dependencies.
                for (component in manifestInitializers) {
                    buildTree(component)
                }

                // Tree is build, initialise each of the components.
                println("pre-runBlocking")
                runBlocking {
                    initializeTree()
                }
                println("post-runBlocking")
            }
        } catch (exception: ClassNotFoundException) {
            throw BoltException(exception)
        }
    }

    /**
     * For a given BoltInitializer instance parse its
     * dependencies and add them to the dependency tree.
     *
     * This method is called recursively.
     */
    private fun buildTree(
        component: Class<out BoltInitializer>,
    ) {
//        println("buildTree : ${component.name}")

        if (boltInitializerTreeRoot.findNode(component.name) == null) {
            try {
                val initializer =
                    component.getDeclaredConstructor().newInstance() as BoltInitializer
                val dependencies = initializer.dependencies()

                val newNode = BoltTreeNode(component.name)

                if (dependencies.isNotEmpty()) {

//                    println("dependencies : ${component.name}")
                    for (clazz in dependencies) {
//                        println("dep : ${clazz.name}")

                        buildTree(clazz)

                        boltInitializerTreeRoot.findNode(clazz.name)?.let {node ->
                            node.dependencies.add(newNode)
                        }
                    }

                } else {
//                    println("add node to root.")

                    // This component has no dependencies, so add this node to the root of the tree
                    boltInitializerTreeRoot.dependencies.add(newNode)
                }

            } catch (throwable: Throwable) {
                throw BoltException(throwable)
            }
        }
    }

    /**
     * Parse dependency tree and initialise all the
     * components together at a given depth in the tree.
     */
    private suspend fun initializeTree() {
        println("initializeTree")

        boltInitializerTreeRoot.printDependencyTree()

        boltInitializerTreeRoot.traverseBreadthFirst().forEachIndexed { depth, nodesAtDepth ->
            coroutineScope {
                // We don't initialise the root node, so skip it.
                if (depth != 0) {
                    println("Depth $depth: $nodesAtDepth")

                    val deferredJobs = nodesAtDepth.map { node ->
//                        println("node : ${node.name}")
                        val component = Class.forName(node.name) as Class<BoltInitializer>
                        val initializer =
                            component.getDeclaredConstructor().newInstance() as BoltInitializer
                        async { initializer.create(context) }
                    }
                    println("pre-await")
                    deferredJobs.awaitAll()
                    println("post-await")
                }
            }
        }

    }

    /**
     * Create a list of nodes for each depth in our tree. Best case scenario is
     * a single depth (one list), worst case is "n" depth, where we have a lot
     * of lists, each with a single node.
     */
    private fun BoltTreeNode.traverseBreadthFirst(): List<List<BoltTreeNode>> {
        val result = mutableListOf<MutableList<BoltTreeNode>>()
        val queue = mutableListOf<Pair<BoltTreeNode, Int>>()
        queue.add(this to 0)
        while (queue.isNotEmpty()) {
            val (current, depth) = queue.removeFirst()
            if (depth >= result.size) {
                result.add(mutableListOf())
            }
            result[depth].add(current)
            current.dependencies.forEach { dependency ->
                queue.add(dependency to depth + 1)
            }
        }
        return result
    }


    /**
     * The dependency tree we create isn't sorted or balanced, so we
     * search every node until we find the one we're looking for.
     */
    private fun BoltTreeNode.findNode(targetName: String): BoltTreeNode? {
        if (this.name == targetName) {
            return this
        }
        for (dependency in this.dependencies) {
            val foundNode = dependency.findNode(targetName)
            if (foundNode != null) {
                return foundNode
            }
        }
        return null
    }

    /**
     * Print a visual representation of the dependency tree.
     */
    private fun BoltTreeNode.printDependencyTree(indent: String = "") {
        println(indent + this.name)
        for (dependency in this.dependencies) {
            dependency.printDependencyTree("$indent  ")
        }
    }
}