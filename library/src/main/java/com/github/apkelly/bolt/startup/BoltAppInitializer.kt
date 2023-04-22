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

    private val mDiscovered: MutableSet<Class<out BoltInitializer?>> = mutableSetOf()

    private data class BoltTreeNode(
        val name: String,
        val dependencies: MutableList<BoltTreeNode> = mutableListOf()
    )

    private val boltInitializerTreeRoot = BoltTreeNode(name = "root")

    companion object {
        fun getInstance(context: Context): BoltAppInitializer {
            return BoltAppInitializer(context)
        }
    }

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
                            mDiscovered.add(component)
                        }
                    }
                }

                for (component in mDiscovered) {
                    buildTree(component)
                }

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

    private fun buildTree(
        component: Class<out BoltInitializer>,
    ) {
        println("buildTree : ${component.name}")

        if (boltInitializerTreeRoot.findNode(component.name) == null) {
            try {
                val initializer =
                    component.getDeclaredConstructor().newInstance() as BoltInitializer
                val dependencies = initializer.dependencies()

                val newNode = BoltTreeNode(component.name)

                if (dependencies.isNotEmpty()) {

                    println("dependencies : ${component.name}")
                    for (clazz in dependencies) {
                        println("dep : ${clazz.name}")

                        buildTree(clazz)

                        boltInitializerTreeRoot.findNode(clazz.name)?.let {node ->
                            node.dependencies.add(newNode)
                        }
                    }

                } else {
                    println("add node to root.")

                    // This component has no dependencies, so add this node to the root of the tree
                    boltInitializerTreeRoot.dependencies.add(newNode)
                }

            } catch (throwable: Throwable) {
                throw BoltException(throwable)
            }
        }
    }

    private suspend fun initializeTree() {
        println("initializeTree")

        boltInitializerTreeRoot.printDependencyTree()

        boltInitializerTreeRoot.traverseBreadthFirst().forEachIndexed { i, d ->
            println("Depth $i: $d")

            coroutineScope {
                if (i != 0) {
                    val deferredJob = d.map { node ->
                        println("node : ${node.name}")
                        val component = Class.forName(node.name) as Class<BoltInitializer>
                        val initializer =
                            component.getDeclaredConstructor().newInstance() as BoltInitializer
                        async { initializer.create(context) }
                    }
                    println("pre-await")
                    deferredJob.awaitAll()
                    println("post-await")
                }
            }
        }

    }

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

    private fun BoltTreeNode.printDependencyTree(indent: String = "") {
        println(indent + this.name)
        for (dependency in this.dependencies) {
            dependency.printDependencyTree("$indent  ")
        }
    }
}