TreeLock
========
Random idea I had in a concurrent programming class to make a mutex. The idea is to have a tree of locks, each process starts at the branches and acquires locks until it gets to the root.  Probably no better in performance than the standard library but it was fun to build.
