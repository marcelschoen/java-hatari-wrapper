# java-hatari-wrapper

Java library wrapping the "Hatari" Atari ST emulator, making it easy to use from within Java

## Status

It's work in progress. As of right now, it only works on Windows (possibly only on Windows 10, not tested on other versions).
While the Linux version _should_ work (the Linux "Hatari" is included), it fails in the JNA library, when determining
the desktop windows handles. For some reason, this currently doesn't work, at least not on my Pop OS Linux. Any 
help to get this working is appreciated.

## Dependency

This library is NOT available in Maven Central for the time being (as I am too lazy to go through the
whole deployment process). Just build it locally (see below) and then add this dependency to your POM:

```
<dependency>
    <artifactId>games.play4ever.retrodev</artifactId>
    <groupId>hatari-wrapper</groupId>
    <version>...CURRENT LIBRARY VERSION...</version>
</dependency>
```

## Running The Emulator

To manually run the emulator, execute this command after building the jar:

`
$ java -jar hatari-wrapper-<version>.jar
`

The purpose of executing the jar and starting the emulator like this is merely to demonstrate
how it works, and to test IF it works on your given platform. Normally, you would include
this jar file as a normal dependency in your own Java project, and then use the API to
start the emulator.

## API

To prepare the working directory of the emulator, call this method:

`
Hatari.prepare(new File("./hatari"), Hatari.TOS.tos206);
`

where the first parameter is a File object with the directory location (will be created),
and the second is the TOS version to use (the corresponding image will be unpacked).

This step is needed at least once. After that, you can run the emulator with this method:

```
MACHINE machine = MACHINE.ste;
MEMORY memory = MEMORY.mb1;
MODE mode = MODE.low;
startEmulator(INSTANCES.testing, machine, memory, mode, machine.hasBlitter, null, fileToCopy);
```

For details about the parameters, consult the javadoc.

_TODO - remove "instances" parameter, update description_

## Building

This library requires Maven to be built. Just clone the repository, and in the "java-hatari-wrapper" run

`
$ mvn install
`

That's it - the library is now available in your local Maven repository to be used in your own project.