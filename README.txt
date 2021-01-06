
MOS TOOLKIT CLASSES AND EXTENTIONS FOR SUPERCOLLIDER
=============================================================

SuperCollider classes and extentions: Resource management, type checking,
real-time controls, immediate reflection and feedback, simplify technical
aspects of composition.

                                        v0.5
                                        September 2020

Contents
- - - - - - - - -
  OVERVIEW
  INSTALLING MOS TOOLKIT
  GETTING STARTED (WHERE TO FIND DOCUMENTATION?) 
  EXPLORING MOS TOOLKIT
  WHAT NEXT?
  VERSION HISTORY




#------------------------------------------ -o-
OVERVIEW


The MOS Toolkit intends to present existing SuperCollider functionality in
the simplest possible way, allowing complex actions to be reduced to the
smallest possible number of operations while managing an arbitrary degree
of complexity, ideally with a single line of code.  The MOS Toolkit
implements a suite of classes under a new root class, MobileSound, which is
coordinated with class extentions throughout the core class heirarchy.

The MOS Toolkit may be used anywhere that sclang commands can be executed.
Terminal driven interaction brings extra convenience, especially with an
editor that is capable of mapping key strokes to tokens under the cursor.

Although MOS Toolkit provides new features, its primary goal is to support,
simplify and streamline SuperCollider development for any purpose.


MOS functionality includes:

  * Resource management including Nodes, Busses, Buffers, Routines
  * Namespace management including Groups, Files
  * Type checking and error checking for classes and Arrays
  * OSC message monitoring and filtering
  * Multi-level logging from methods and functions
  * Regular use of thread synchronization to serialize asynchronous tasks
  * Dashboard text presentation of system state, 
      the state of any instrumented class or instance, 
      unified presentation of primitive types and containers, 
      quick lookup of class structure and method signatures
  * Management of select GUI elements from sclang
  * Quick access to SCDoc documentation in any browser 
  * Discrete and continuous iterative control of class variables 


MOS core classes include:

  Curve               -- Synth driven envelope via a Bus 
  IterationParameter  -- Function driven iterative values 

  BufferCache         -- Cache management of loaded Buffers 
  OSC                 -- Monitoring and filtering of OSC activity

  Dump                -- Class reflection and variable presentation
  Log                 -- Context sensitive logging
  MOSErrorClasses     -- Exception subclasses
  Parse               -- Variable parsing and error checking
  Z                   -- Miscellaneous functionality: 
                           system, documentation, presentation

  MobileSound         -- Suite and resource namespace management
  Suites              -- Root for non-general classes

Initially, the class Suites contains a single class, LongSample, which
serves as a concrete example of how the MOS Toolkit may be used.

See below for detail on using the class extentions.




#------------------------------------------ -o-
INSTALLING MOS TOOLKIT


The MOS Toolkit was developed under SuperCollider v3.6.6, then updated for
SuperCollider v3.9.3, on macOS v10.12.2.  However, it is expected to work
without difficulty on newer versions and other platforms.

For those new to SuperCollider, first... install SuperCollider:

  https://github.com/supercollider/supercollider
  http://sccode.org


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




#------------------------------------------ -o-
GETTING STARTED (WHERE TO FIND DOCUMENTATION?) 


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




#------------------------------------------ -o-
EXPLORING MOS TOOLKIT


What follows is a partial listing of MOS Toolkit functionality.  
Hopefully, the most essential parts.

To get a list of (almost) EVERYTHING in the MOS Toolkit core classes and
the MOS Toolkit extentions, use the following methods:

  MobileSound.catalogSubclasses()
  MobileSound.catalogInstanceElementsOfMOSExtendedClasses()

(Unfortunately, the latter method does not list class methods defined in
the extentions.  .classAnatomy will display them, albeit mixed in with
everything else.)

In general, any code that wants to be written more than once will be scoped
to an appropriate class and written as a method extention.  Naturally,
core system classes (Buffer, File, Object, Node/Synth, Server, String) and
common containers are good places to express class extentions.


In the notation below...
  . Leading asterisks (*) indicate Class objects (versus instances);
  . Parenthesis (()) indicate optional spellings with similar functionality;
  . Brackets ([]) indicate aliases.



CLASS REFLECTION AND SELF-DOCUMENTATION ----------------------------------

The following methods may be used anywhere:

  (*) .classAnatomy      [.cla]
  (*) .methodSignatures  [.sigs]

  (*) .pr(r)etty         [.pr(r)]


To leverage .pretty (and its variations), a class must be intentionally
instrumented to to so.  By default, .pretty() returns only the class name.

For cases where too-much-information is available, but not always
appropriate, it can be put into the (slightly longer) method named
.prretty.

.pretty is an example of a "MOS Toolkit protocol".  See its declaration 
in Object and its use throughout the suite.  It has a tortured, but
effective, set of arguments that cover most cases and which are
parsimoniously used in practice.



NAMESPACE AND RESOURCE MANAGEMENT ----------------------------------------

"Namespace" refers to any resource that has a heirarchical ordering.
Such as pathnames, OSC paths, node graphs.  The following classes and
methods help to manage namespaces and enforce resource management:

  * .workingDirectory
  BufferCache
  MobileSound.createGroup()
  Node.newRegistered() 
  OSC.oscPath*
  Routine.spinlock() 

All resources have their place in the heirarchy.  MOS Toolkit specific
resources will always be rooted (at some arbirary depth) under some
logical equivalent of "mos".  Eg: LongSample.group is under
MobileSound.group is under the RootNode.  Pathnames are more concrete and
actually contain the string "mos".  Even Synth names, eg:
\mosSynthCurveExponentialUpwards.


By default, Buffer.load() uses BufferCache, though it can be turned off.

Node.newRegistered() ensures that every element in the node graph has a
NodeWatcher and, therefore, can be started or stopped as necessary to save
power.

Routine.spinlock() aspires to be a one method-fits-all solution for all
variations of inter-fork (thread) dependence based upon well-known objects
with consistent state behavior.



MOS TOOLKIT PROTOCOLS AND CONVENTIONS ------------------------------------

"Protocol" in this context means convention or standard operating
procedure.  Protocols are conventions that all MOS Toolkit custom classes
should follow.  None are required... except when they are needed.

   *  .demo
   *  .group  (Use MobileSound.createGroup().)
  (*) .mosVersion
  (*) .pr(r)etty  [.pr(r)]
   *  .usage
   *  .workingDirectory

There may be others...


Coding conventions:

  . Return values: As a rule all status reports, no matter how large, 
    are returned as strings.  The calling environment can handle it as it
    sees fit.  (Eg: Return string verbatim or caches it for
    conditional reporting.)
    All functions or class methods that do not return a value must
    return an empty Symbol ('' or ^'').

  . As a rule, (simple) String inputs can be passed in as Symbols.

  . As a rule, never use default arguments.  Always use ?? to set nil
    input arguments to something acceptable.

  . Use Parse to validate all inputs.  Especially in setters, upon
    which the initialization methods rely heavily.

  . Declare all new Synths with .newRegistered().  Set paused:true unless 
    it will be running immediately upon definition.

  . String is naturally a great place to express certain system operations,
    though the heavy lifting should be expressed in the appropriate
    class and referenced from String.


Classes as development tools:
  
  . Log -- Provides useful state updates in any context.

  . Parse -- Makes code and operational environments more
    robust and maintanable by quickly and precisely identifying bad inputs.

  . Curve and IterationParameter -- Make instances smarter and
    more flexible in real-time performance environments.

  . BufferCache -- Manage critical memory resources in
    sample-rich performance environments.

  . OSC -- Filter the output of Server activity.

  . Dump and Z -- Support all of the above and endeavor to have a few useful
    tricks of their own.


The code contains structural hints in the comments, including: 
  ALIAS DEBUG DEFAULTS FIX FORKS LOGS NB NEEDS ROUTINE Q RELEASE SPINLOCK
  TARGET STATUS TBD XXX



EXPLORING AND MONITORING THE SYSTEM ------------------------------------

  Z.dashboardSystem

  Z.helpGenerateURL(CLASS-or-PATTERN, [patternAsClass:true])

  Z.openTerminal(CLASS-or-FILEPATH, [filepathAsClass:true])

  Z.helpBrowse
  Z.dashboardBrowseObjects



DISPLAYING TYPES ---------------------------------------------------------

This sub-section is concerned with both primitive and compound types as
well as dashboard-like presentation of the state of complex classes.


.pretty is used to gratuitously format some types into a "canonical" form.
Eg: Evaluating "42.0.pr" will return "42.0" because it is clearly a Float.
Other examples include NetAddr.pretty() and Symbol.pretty().

At the other end of the spectrum, .pretty() is used to present
dashboard-like feedback on complex classes.  See: Server, Platform.

Naturally, .pretty() has a role in all container classes, as a distributed
means of always outputting the content of complex, custom data structures.
The best way to experiment with this is to suffix ".pr" to any wicked data
structure you have developed.  Let me know (or send a pull request!) if it
not as useful as you'd like.  Please also explore the arguments to
.pretty() as a means to manage your output or drill down on its contents.

Simplicity should rise over complexity in all cases!


Classes also constitute a type.  For these, Object has been extended with
methods that engage the methods already provided by sclang:

  .hasClassMethod()
  .hasClassVariable()
  .hasInstanceMethod()
  .hasInstanceVariable()
  .hasMethod()
  .hasRespondingInstanceMethod()
  .hasRespondingMethod()
  .hasVariable()




#------------------------------------------ -o-
WHAT NEXT?


MOS Toolkit is a living project, forever evolving and changing (albeit slowly).

In addition to continued development of core classes, extentions and Suites,
specific items on the TO-DO list include...

  . Extend Curve to more complex envelopes and allow "chaining" of Curves.

  . Use Semaphore to resolve race conditions, notably in
      MobileSound.createGroup and BufferCache search and store.

  . Better use of sclang Errors and Exceptions.

  . Deep test on other platforms, notably Linux.

  . Create SCDoc-umentation

  . Create a Quark

  . Rewrite select functionality as C++ primitives




#------------------------------------------ -o-
VERSION HISTORY


v0.4.1 -- September 2018
  Minor changes for compatibility with SuperCollider v3.9.3.

v0.4 -- August 2018
  Initial release.  MOS Toolkit Foundation.

