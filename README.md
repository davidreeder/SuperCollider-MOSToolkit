
# MOS Toolkit Classes and Extentions for SuperCollider

                                        v0.4
                                        August 2018

Table of Contents                                 |
-----------------                                 |
  OVERVIEW                                        |
  INSTALLING MOS TOOLKIT                          |
  GETTING STARTED (WHERE TO FIND DOCUMENTATION?)  |
  EXPLORING MOS TOOLKIT                           |
  WHAT NEXT?                                      |




## OVERVIEW


The MOS Toolkit intends to combine and present existing SuperCollider
functionality in the simplest possible way, allowing complex actions to be
reduced to the smallest possible number of operations while managing an
arbitrary degree of complexity, ideally with a single line of code.
The MOS Toolkit implements a suite of core classes under the root class,
MobileSound, coordinated with a variety of class extentions throughout
the class heirarchy.

The MOS Toolkit may be used anywhere that sclang commands can be executed.
Though it favors terminal driven interaction, especially with an editor
that can map key strokes to code parameterized by text under the cursor.

Although MOS Toolkit endeavors to provide some interesting features in its
own right, its primary goal is to support, simplify and streamline
SuperCollider development for any purpose.


MOS functionality includes:

  * Resource management including **Node**s, **Bus**ses, **Buffer**s, **Routine**s 
  * Namespace management including **Group**s, **File**s
  * Type checking and error checking for classes and **Array**s
  * OSC message monitoring and filtering
  * Multi-level logging from methods and functions
  * Regular use of thread synchronization to serialize asynchronous tasks
  * Dashboard text presentation of system state, 
      the state of any instrumented class or instance, 
      unified presentation of primitive types and containers, 
      quick lookup of class structure and method signatures
  * Management of select GUI elements from sclang
  * Quick access to **SCDoc** documentation in any browser 
  * Discrete and continuous iterative control of class variables 


MOS core classes include:

* **Curve**               :: **Synth** driven envelope via a **Bus**
* **IterationParameter**  :: **Function** driven iterative values
* **BufferCache**         :: Cache management of loaded **Buffer**s
* **OSC**                 :: Monitoring and filtering of OSC activity
* **Dump**                :: **Class** reflection and variable presentation
* **Log**                 :: Context sensitive logging
* **MOSErrorClasses**     :: **Exception** subclasses
* **Parse**               :: Variable parsing and error checking
* **Z**                   :: Miscellaneous functionality: system, documentation, presentation 
* **MobileSound**         :: Suite and resource namespace management
* **Suites**              :: Root for non-general classes

Initially, the class **Suites** contains a single class, **LongSample**,
which serves as a concrete example of how the MOS Toolkit may be used.

See below for detail on using the class extentions.




## INSTALLING MOS TOOLKIT


The MOS Toolkit was developed under SuperCollider v3.6.6 on macOS v10.12.2.
However, it is expected to work without difficulty on newer versions and
other platforms.

For those new to SuperCollider, first... install SuperCollider:

* [https://github.com/supercollider/supercollider](https://github.com/supercollider/supercollider)
* [http://sccode.org](http://sccode.org)


Then, put the directory containing this README into one of the
SuperCollider Extensions directories.  Find these locations by running the
following lines of code:

    Platform.systemExtensionDir
    Platform.userExtensionDir

On macOS these are:

    /Library/Application Support/SuperCollider/Extensions
    ~/Library/Application Support/SuperCollider/Extensions


Finally, reload the class libraries or, alternatively, simply restart
SuperCollider.




## GETTING STARTED (WHERE TO FIND DOCUMENTATION?) 


The best way to learn the MOS Toolkit it to use it.  One of its primary
goals is self-documentation, both in the code and from sclang via
reflection and other MOS Toolkit protocols.

Some of the classes have demos that show how they work and provide
initial coding examples.

The headers of each class are intended to give a general summary followed
by obsessive detail about internal operations.

There are also a number of common methods that apply to all classes, some
of which are listed in the following sections.


To get started, try...

    //
    MobileSound.mosVersions
                ...To see all the classes affected by the MOS Toolkit.

    MobileSound.mosDemoList
                ...To see all the classes that contain demos.


    //
    s.boot
    Curve.demo
                ...Run one of the demos.

    Curve.cla()
                ...List all variables and methods in this class.
    Curve.sigs()
                ...List signatures for all methods in this class.

    Curve.sigs(\run)
                ...Search signatures with a specific pattern.
                   (In general, search arguments can be Strings or Symbols.)

    //
    Curve.pretty()
                ...Display general capabilities of a class.

    c = Curve()
    c.pretty()
                ...Display operational parameters of an instance.

    Curve.usage
                ...Get a usage statement.


    //
    Server.pretty()
    Platform.pretty()
                ...Some common classes and instances have been updated
                     to list capabilities and/or operational parameters.

    Platform.pretty(\extension)
    Platform.pretty(\support)
                ...Another way to find the class extension directories.

    SequenceableCollection.cla
    SequenceableCollection.sigs
                ...List everything that SequenceableCollections can do.


    //
    Z.dashboardSystem
                ...Bring a collection of GUI widgets to the foreground.

    Z.helpGenerateURL(SynthDef, patternAsClass:true)
                ...Get help on a particular class.
                     (Especially useful if mapped to a hotkey!)

    Z.openTerminal(Pattern, filepathAsClass:true)
                ...Lookup the code for a class.

    Z.helpBrowse
    Z.dashboardBrowseObjects
                ...General help on all things.




## EXPLORING MOS TOOLKIT


What follows is a partial listing of MOS Toolkit functionality.  
Hopefully, the most essential parts.

To get a list of (almost) EVERYTHING in the MOS Toolkit core classes and
the MOS Toolkit extentions, use the following methods:

    MobileSound.catalogSubclasses()
    MobileSound.catalogInstanceElementsOfMOSExtendedClasses()

(Unfortunately, the latter method does not list class methods defined in
the extentions.  `.classAnatomy` will display them, albeit mixed in
with everything else.)

In general, any code that wants to be written more than once will be scoped
to an appropriate class and written as a method extention.  Naturally, core
system classes (**Buffer**, **File**, **Object**, **Node**/**Synth**,
**Server**, **String**) and common containers are good places to express
class extentions.


In the notation below...

  * Leading asterisks (*) indicate **Class** objects (versus instances);
  * Parenthesis (()) indicate optional spellings with similar functionality;
  * Brackets ([]) indicate aliases.



### _______ CLASS REFLECTION AND SELF-DOCUMENTATION _______

The following methods may be used anywhere:

  * (*) `.classAnatomy`      [`.cla`]
  * (*) `.methodSignatures`  [`.sigs`]
  * (*) `.pr(r)etty`         [`.pr(r)`]


To leverage `.pretty` (and its variations), a class must be intentionally
instrumented to to so.  By default, `.pretty()` returns only the class name.

For cases where too-much-information is available, but not always
appropriate, it can be put into the (slightly longer) method named
`.prretty`.

`.pretty` is an example of a "MOS Toolkit protocol".  See its declaration 
in Object and its use throughout the suite.  It has a tortured, but
effective, set of arguments that cover most cases and which are
parsimoniously used in practice.



### _______ NAMESPACE AND RESOURCE MANAGEMENT _______

"Namespace" refers to any resource that has a heirarchical ordering.
Such as pathnames, OSC paths, node graphs.  The following classes and
methods help to manage namespaces and enfoce resource management:

  * * `.workingDirectory`
  *   **BufferCache**
  *   `MobileSound.createGroup()`
  *   `Node.newRegistered()` 
  *   `OSC.oscPath*`
  *   `Routine.spinlock()` 

All resources have their place in the heirarchy.  MOS Toolkit specific
resources will always be rooted (at some arbirary depth) under some logical
equivalent of "mos".  Eg: `LongSample.group` is under `MobileSound.group`
is under the **RootNode**.  Pathnames are more concrete and actually
contain the string "mos".  Even **Synth** names, eg:
`\mosSynthCurveExponentialUpwards`.


By default, `Buffer.load()` uses **BufferCache**, though it can be turned
off.

`Node.newRegistered()` ensures that every element in the node graph has a
NodeWatcher and, therefore, can be started or stopped as necessary to save
power.

`Routine.spinlock()` aspires to be a one method-fits-all solution for all
variations of inter-fork (thread) dependence based upon well-known objects
with consistent state behavior.



### _______ MOS TOOLKIT PROTOCOLS AND CONVENTIONS _______

"Protocol" in this context means convention or standard operating
procedure.  Protocols are conventions that all MOS Toolkit custom classes
should follow.  None are required... except when they are needed.

  * *   `.demo`
  * *   `.group`  (Use `MobileSound.createGroup()`.)
  * (*) `.mosVersion`
  * (*) `.pr(r)etty`  [`.pr(r)`]
  * *   `.usage`
  * *   `.workingDirectory`

There may be others...


Coding conventions:

  * Return values: As a rule all status reports, no matter how large, 
    are returned as strings.  The calling environment can handle it as it
    sees fit.  (Eg: Return it verbatim as a string or caching it for other
    uses.)  All functions or class methods that do not return a value must
    return an empty Symbol (`''` or `^''`).

  * As a rule, String inputs can be expressed as Symbols.

  * As a rule, never use default arguments, always use ?? to set a nil
    input argument to something acceptable.

  * Use Parse to validate all inputs.  Always.  Especially in setters, upon
    which the initialization methods rely heavily.

  * Declare all new **Synth**s with `.newRegistered()`.  Set paused:true
    unless it is absolutely clear that it will be running immediately upon
    definition.

  * String is naturally a great place to express certain system operations,
    though the really heavy lifting should be expressed in the appropriate
    class and referenced from String.


Classes as development tools:
  
  * **Log** is intended to provide useful state updates in any context.

  * **Parse** is intended to make code and operational environments more
    robust and maintanable by quickly and precisely identifying bad inputs.

  * **Curve** and **IterationParameter** are intended to make instances
    smarter and more flexible in real-time performance environments.

  * **BufferCache** is intended to manage critical memory resources in
    sample-rich performance environments.

  * **OSC** is intended to filter the output of **Server** activity.

  * **Dump** and **Z** support all of the above and endeavor to have a few
    useful tricks of their own.


The code may contain commented hints, including:

  * ALIAS
  * DEBUG
  * DEFAULTS
  * FIX
  * FORKS
  * LOGS
  * NEEDS ROUTINE
  * Q
  * RELEASE
  * SPINLOCK TARGET
  * STATUS
  * TBD
  * XXX



### _______ EXPLORING AND MONITORING THE SYSTEM _______

  * `Z.dashboardSystem`
  * `Z.helpGenerateURL(CLASS-or-PATTERN, [patternAsClass:true])`
  * `Z.openTerminal(CLASS-or-FILEPATH, [filepathAsClass:true])`
  * `Z.helpBrowse`
  * `Z.dashboardBrowseObjects`



### _______ DISPLAYING TYPES _______

This sub-section is concerned with both primitive and compound types as
well as dashboard-like presentation of the state of complex classes.


`.pretty` is used to gratuitously format some types into a "cannonical"
form.  Eg: Evaluating `42.0.pr` will return *42.0* because it is clearly a
**Float**.  Other examples include `NetAddr.pretty()` and
`Symbol.pretty()`.

At the other end of the spectrum, `.pretty()` is used to present
dashboard-like feedback on complex classes.  See: **Server**, **Platform**.

Naturally, `.pretty()` has a role in all container classes, as a
distributed means of always outputting the content of complex, custom data
structures.  The best way to experiment with this is to suffix `.pr` to any
wicked data structure you have developed.  Let me know (or send a pull
request!) if it not as useful as you'd like.  Please also explore the
arguments to `.pretty()` as a means to manage your output or drill down on
its contents.

Simplicity should rise over complexity in all cases!


Classes also constitute a type.  For these, **Object** has been extended
with methods that simply engage the methods already provided by sclang:

  * `.hasClassMethod()`
  * `.hasClassVariable()`
  * `.hasInstanceMethod()`
  * `.hasInstanceVariable()`
  * `.hasMethod()`
  * `.hasRespondingInstanceMethod()`
  * `.hasRespondingMethod()`
  * `.hasVariable()`




## WHAT NEXT?


MOS Toolkit is a living project, forever evolving and changing.

In addition to continued development of core classes, extentions and Suites,
specific items on the TO-DO list include...

  * Test with current version of SuperCollider!

  * Extend **Curve** to more complex envelopes and allow "chaining" of
    **Curve**s.

  * Use **Semaphore** to resolve race conditions, notably in
    `MobileSound.createGroup` and **BufferCache** search and store.

  * Better use of sclang **Errors** and **Exceptions**.

  * Deep test on other platforms, notably Linux.

  * Create **SCDoc**-umentation

  * Create a Quark

  * Rewrite select functionality as C++ primitives

