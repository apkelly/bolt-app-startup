# Bolt-App-Startup
Inspired by the [Google App Startup](https://developer.android.com/topic/libraries/app-startup) library, this project aims to bring async app initialisation to your project.

## The name
Usain Bolt is an Olympic sprinter and possibly the fastest man on the planet.  
So Bolt seemed like a fitting name for a library that aims to initialise your Android app as fast as possible.

## The problem
The App Startup library from Google uses a system of initializers to setup the different dependencies for your Android app.

However, these dependencies are created in series, which means the time to start your app is the sum of all the time taken by each initializer.

## The solution
The Bolt App Startup library uses Kotlin Coroutines to setup the dependencies for your Android app in parallel.

This means an a reduction in the time taken to launch your app.

## How it works

Rather than initialising each dependency in sequence, as done by the Google library, this library builds up a dependency tree.

This tree is parsed in a breadth first search, and the optimisations come from our ability to initialise all the nodes (or dependencies) at a given depth at the same time.

Once a given depth has been initialised, we move on to the next one, until there are not more.

## The improvements

In a perfect setup, none of the dependencies depend on each other. This means the total startup time is time taken to initialise the slowest node.

In the worst setup, every dependency depends on another one. This means the total startup time is the sum of all the time taken to initialise each node. This is the same time as taken by the Google App Startup library.

In a more typical setup, some dependencies will depend on others, and some will depend on nothing. This means the total startup time is the sum  of the time takes to initialise the slowest node at each depth in the tree.
