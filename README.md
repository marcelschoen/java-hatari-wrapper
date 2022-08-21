# java-hatari-wrapper

Java library wrapping the "Hatari" Atari ST emulator, making it easy to use from within Java

Hatari version: 2.4.1.

## Status

It's work in progress. As of right now, it only works on Windows (possibly only on Windows 10, not tested on other versions).
and, in a slightly reduced fashion, on Linux. The Linux version is a bit less robut because the JNA library used to control
the emulator desktop window (and force it into the foreground) works only on Windows. 

However, launching the emulator should work fine on any recent Linux that supports SDL 2.0.

Also, MacOS is not supported at all, because I don't have a Mac and don't really care for it. Feel free to
add support for MacOS yourself - pull requests are always welcome. I'll also help you if you have questions
regarding the code.

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
HatariInstance testing = new HatariInstance("testing",
        true,
        false,
        true,
        true,
        true,
        true,
        MachineType.ste,
        TOS.tos206,
        ScreenMode.low,
        Memory.mb1);

DesktopWindow emulatorWindow = HatariWrapper.startEmulator(testing);
```

The "DesktopWindow" instance allows to push the window into the foreground (uses the JNA library),
to be able to send it keyboard input using the Java Robot API (for certain use-cases, where the emulator
is used to implement a build process). But NOTE: This JNA functionality currently works on Windows only,
so Linux support is limited there.

And then to stop and close the emulator later:

```
HatariWrapper.stopEmulator(testing);
```

## Building

This library requires Maven 3.x and Java 11 to be built. Just clone the repository, and in the "java-hatari-wrapper" run

`
$ mvn install
`

That's it - the library is now available in your local Maven repository to be used in your own project.