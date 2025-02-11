#!/bin/bash

javac -sourcepath constr -d .classes constr/Build.java
java -classpath .classes Build

