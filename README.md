# java-hatari-wrapper

Java library wrapping the "Hatari" Atari ST emulator, making it easy to use from within Java

## Status: Work in Progress

Should be available latest September 2022.


## Dependency

This library is NOT available in Maven Central for the time being (as I am too lazy to go through the
whole deployment process). Just build it locally (see below) and then add this dependency to your POM:

```
<dependency>
    <artifactId></artifactId>
    <groupId></groupId>
    <version>...CURRENT LIBRARY VERSION...</version>
</dependency>
```


## Building

This library requires Maven to be built. Just clone the repository, and in the "java-hatari-wrapper" run

`
$ mvn install
`

That's it - the library is now available in your local Maven repository to be used in your own project.