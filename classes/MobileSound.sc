//
// MobileSound.sc
//
// Superclass and root of MobileSound core classes.
// See also mos/extensions.
//
// NB  NOT threadsafe!
//
//
// PUBLIC PROPERTIES--
//   <> *appBrowser   <> *appBrowserPreference  
//   <> *appBrowserDefault     
//   <  *appTextEdit
//
//
// PUBLIC CLASS METHODS--
//   *mosVersion  *suiteVersion
//
//   *mosVersions
//   *mosExtendedClasses
//   *mosDemoList
//
//   *catalogInstanceElementsOfMOSExtendedClasses  
//   *catalogSubclasses  
//
//   (*)createGroup
//   (*)releaseGroup
//   (*)isGroupPlaying  (*)isGroupRunning
//
//
// HELPER METHODS--
//   *isGroupActiveTest
//
//
// MOS PROTOCOL SUPPORT--
//   *pretty
//
//
// MOS DEPENDENCIES--
//   File_MOS
//   Group_MOS
//   Object_MOS
//   Routine_MOS
//   String_MOS
//   Z
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------

MobileSound 
{
  classvar     suiteVersion  = "0.4",   //RELEASE
               classVersion  = "0.3";   //RELEASE


  classvar  <> appBrowserPreference  = "/Applications/Firefox.app",
            <> appBrowserDefault     = "/Applications/Safari.app",

            <  appTextEdit           = "/Applications/TextEdit.app";


  classvar  <  baseDirname,
            <  workingDirectory;


  classvar  <> group;




  //--------------------------------------------------------------- -o-
  // Class lifecycle, identity, reflection.

  //------------------------ -o-
  *initClass  
  {
    Class.initClassTree(PathName);

    //
    this.group = nil;

    baseDirname       = "mos";
    workingDirectory  = Platform.userDocuments +/+ baseDirname;

    //
    StartUp.add  {
      File.createDirectory(workingDirectory);  //XXX  Best effort!
    };

  }


  //------------------------ -o-
  *mosVersion    { |classVersion_|
    ^("% v%").format(this.asString, classVersion_ ?? classVersion); 
  }


  //------------------------ -o-
  *suiteVersion  { ^("% suite v%").format(this.asString, suiteVersion); }




  //---------------------------------------------------------- -o--
  // Getters/setters.

  //------------------------ -o-
  *appBrowser  
  {
    if (this.appBrowserPreference.pathExists, {
      ^this.appBrowserPreference;
    });


    //
    Log.error(thisFunction, 
                "Cannot find appBrowserPreference.  (%)", 
                this.appBrowserPreference
             ).postln; 

    if (this.appBrowserDefault.pathExists, {
      ^this.appBrowserDefault;
    });


    //
    Log.error(thisFunction, 
                "Cannot find appBrowserDefault.  (%)", 
                this.appBrowserDefault
             ).postln; 

    ^nil;
  }

  *appBrowser_  { |browserPath| 
    if (browserPath.pathExists, {
      this.appBrowserPreference = browserPath;

    }, {
      Log.error(thisFunction, 
            "browserPath DOES NOT EXIST, using appBrowserDefault.  (%)", 
            browserPath
         ).postln; 
    });

    ^this;
  }




  //--------------------------------------------------------------- -o-
  // Public class methods.

  //------------------------ -o-
  *mosVersions  {                                       //LOGS
    var  allMOSClasses,
         classTraversalFunction;

    var  str;
    

    //
    classTraversalFunction = {  | thisClass, 
                                  indentString, 
                                  indentWidth, 
                                  indentWidthIncrement
                                |
      var  rval  = false;

      if (thisClass.hasMethod(\mosVersion), {
        ("%%").format(
                 indentString.dup(indentWidth).join, 
                 thisClass.mosVersion
               ).postln;

        rval = true;
      });

      rval;
    };


    //
    str = ("%\n").format(MobileSound.suiteVersion);
    str = str ++ ("  sc %\n").format(Z.versionSC);
    str = str ++ ("  osx %\n\n").format(Z.versionApple);

    str.post;

    allMOSClasses = Object.recurseClassesWithFunction(classTraversalFunction);
    "".postln;


    //
    ^allMOSClasses;
  }


  //------------------------ -o-
  *mosExtendedClasses  {
    var  classTraversalFunction;

     
    //
    classTraversalFunction  = { | thisClass, 
                                  indentString, 
                                  indentWidth, 
                                  indentWidthIncrement
                                |
      var  rval  = false;

      if (    thisClass.hasClassMethod(\mosVersion)
           && thisClass.inheritanceString
                          .regexpMatch(": MobileSound:", casei:false).not
           && thisClass.classname.regexpMatch("MobileSound", casei:false).not,
        { rval = true; }
      );

      rval;
    };


    //
    ^Object.recurseClassesWithFunction(classTraversalFunction);
  }


  //------------------------ -o-
  *mosDemoList  {
    var  classTraversalFunction;

     
    //
    classTraversalFunction  = { | thisClass, 
                                  indentString, 
                                  indentWidth, 
                                  indentWidthIncrement
                                |
      var  rval  = false;

      if (    thisClass.hasClassMethod(\demo)
           && thisClass.inheritanceString
                          .regexpMatch(": MobileSound:", casei:false),
        { rval = true; }
      );

      rval;
    };


    //
    ^Object.recurseClassesWithFunction(classTraversalFunction);
  }


  //------------------------ -o-
  *catalogInstanceElementsOfMOSExtendedClasses  {
    var  firstClassnameCharacter,
         clamosOutputOffset;

    var  str  = "";

    
    //
    MobileSound.mosExtendedClasses.do { | elem
                                        |  
      firstClassnameCharacter = elem.classname[0];
      clamosOutputOffset = elem.clamos.find("\n" ++ firstClassnameCharacter);

      if (clamosOutputOffset.isNil, {
        clamosOutputOffset = 0;
      }, {
        clamosOutputOffset = clamosOutputOffset + 1;
      });


      //
      str = ("%\n%\n%\n\n").format(
                              str,
                              Z.sepl, 
                              elem.clamos[clamosOutputOffset..].stripWhiteSpace
                            );  
    };


    //
    ^str;
  }


  //------------------------ -o-
  *catalogSubclasses  {
    var  str  = "";

    MobileSound.subclasses
      .asList.sort({ |x,y|  x.asString < y.asString; }).do { |elem|

      str = ("%\n%\n%\n\n").format(
                              str,
                              Z.sepl, 
                              elem.cla.stripWhiteSpace
                            );  
    };

    ^str;
  }

  
  //------------------------ -o-
  // By DEFAULT...
  //   . MobileSound creates MobileSound.group;
  //   . Subclasses defining <>group create groups under this.superclass.group
  //       + may define group as var, classvar or both,
  //       + DOES not create if group is already defined AND playing...
  //   . Other classes create group under MobileSound.group.
  //
  // ...Otherwise create new group at given target.
  //
  // NB  Should be coupled with Routine.spinlock() in calling environment
  //       in order to discretely assess success or failure.
  //
  //
  // RETURNS  
  //   created (or pre-existing) group  in most cases, EXCEPT...
  //   blank Symbol                     when subclasses create chain of 
  //                                      nested groups; in these cases
  //                                      group is assigned to this.group.
  //
  *createGroup  {  | target, addAction, server,                 //FORKS
                     verbose, 
                     thisInstance,
                     paused
                   |
    var  newGroup  = nil;


    //DEFAULTS.
    addAction  = addAction  ?? \addToHead;
    server     = server     ?? Server.default;
    verbose    = verbose    ?? true;


    // Create MobileSound.group.
    //
    if (MobileSound == this, 
    {
      if (this.isGroupPlaying.not, 
      {
        this.releaseGroup();
        this.group = 
          Group.newRegistered(
                  server.defaultGroup, \addToHead, server, paused:paused);

        if (this.group.isNil, {
          Log.error( thisFunction, 
                     "FAILED to CREATE %.class.group.", this.classname
                   ).postln; 
        });
      });

      if (target.isNil, { ^this.group; });
    });


    // Create object default group...
    //
    if (target.isNil,
    {
      // Create default instance variable group... 
      //   within this.superclass.group.
      // NB IE   Multiple instances ALWAYS under parent classvar this.group.
      // ASSUME  Instances are always leaf nodes in classvar group graphs.
      //
      if (thisInstance.notNil,  
      {
        if (this.hasInstanceVariable(\group).not, {
          Log.error( thisFunction, 
                     "Class % has NO INSTANCE VARIABLE named group.", 
                        this.classname
                   ).postln; 
          ^nil;
        });


        if (thisInstance.isGroupPlaying(verbose:false), 
        {
          if (verbose, {
            Log.warning( thisFunction, 
                         "%.group ALREADY EXISTS.  (%)", 
                              this.classname, thisInstance.group
                       ).postln; 
          });

          ^thisInstance.group;
        });

        //
        {
          this.superclass.createGroup( addAction:  addAction, 
                                       server:     server, 
                                       verbose:    verbose
                                     );

          Routine.spinlock( { this.superclass.isGroupPlaying(); },
                            testFunctionString: 
                              "{ this.superclass.isGroupPlaying(); }",
                            testIterationWait:  0.01,
                            logContext:         thisFunction
                            //, verbose:true
                  );

          target = this.superclass.group;

          if (target.isNil, { 
            Log.warning(thisFunction, 
                     "this.superclass.group is nil.  "
                  ++ "FALLING BACK to server.defaultGroup.  (Instance case.)"
                ).postln;

            target = server.defaultGroup;
          });


          //
          thisInstance.releaseGroup();
          newGroup = Group.newRegistered(
                             target, addAction, server, paused:paused);

          if (newGroup.isNil, {
            Log.error(thisFunction, 
                  "FAILED to CREATE %.group.", this.classname).postln; 
          }, {
            thisInstance.group = newGroup;
          });

        }.fork;

        ^'';
      });  //thisInstance.notNil


      // Create default class variable group... within this.superclass.group.
      //
      // NB  this.hasClassVariable ensures we are referencing this.group,
      //     rather than group inherited from some superclass.
      //
      if (this.hasClassVariable(\group), 
      {
         if (this.isGroupPlaying(verbose:false), 
         {
           if (verbose, {
             Log.warning( thisFunction, 
                          "%.class.group ALREADY EXISTS.  (%)", 
                               this.classname, this.group
                        ).postln; 
           });

           ^this.group;
         });

        //
        {
          this.superclass.createGroup( addAction:  addAction, 
                                       server:     server, 
                                       verbose:    verbose
                                     );

          Routine.spinlock( 
                    { this.superclass.isGroupPlaying(verbose:verbose); },
                    testFunctionString: 
                      "{ this.superclass.isGroupPlaying(verbose:verbose); }",
                    testIterationWait:  0.01,
                    logContext:         thisFunction
                    //, verbose:true
                  );

          target = this.superclass.group;

          if (target.isNil, { 
            Log.warning(thisFunction, 
                     "this.superclass.group is nil.  "
                  ++ "FALLING BACK to server.defaultGroup.  (Class case.)"
                ).postln;

            target = server.defaultGroup;
          });


          //
          this.releaseGroup();
          newGroup = Group.newRegistered(
                             target, addAction, server, paused:paused);

          if (newGroup.isNil, {
            Log.error(thisFunction, 
                  "FAILED to CREATE %.class.group.", this.classname).postln; 
          }, {
            this.group = newGroup;
          });

        }.fork;

        ^'';
      });  //this.hasClassVariable(\group), 


      // Create random group... within MobileSound.group.
      //
      target = MobileSound.group;

      if (target.isNil, { 
        Log.warning(thisFunction, 
              "MobileSound.group is nil.  FALLING BACK to server.defaultGroup."
            ).postln;

        target = server.defaultGroup;
      });

      //
      ^Group.newRegistered(target, addAction, server, paused:paused);

    }); // target.isNil



    // Create some other random group... within target.
    //
    // NB  server.defaultGroup.isPlaying is always false...
    //
    if (target.isPlaying.not && (target != server.defaultGroup), {
      Log.error(thisFunction, 
            "Target group is NOT PLAYING.  Aborting...  (%)", target
          ).postln; 
      ^nil;
    });

    //
    newGroup = Group.newRegistered(target, addAction, server, paused:paused);

    if (newGroup.isNil, {
      Log.error(thisFunction, 
            "FAILED to CREATE group at target %.", target
          ).postln; 
    });

    ^newGroup;
  }


  //INSTANCE
  createGroup  {  |target, addAction, server, verbose, paused|
    ^this.class.createGroup(target, addAction, server, verbose, this, paused);
  }


  //------------------------ -o-
  *releaseGroup  { | thisInstance
                   |
    if (thisInstance.notNil, 
    {
      if (thisInstance.isGroupPlaying, { thisInstance.group.free; });
      thisInstance.group = nil;

      ^'';
    });

    //
    if (this.isGroupPlaying, { this.group.free; });
    this.group = nil;

    ^'';
  }

  releaseGroup  { ^this.class.releaseGroup(this); }   //INSTANCE


  //------------------------ -o-
  *isGroupPlaying  { |verbose, thisInstance|
    ^this.isGroupActiveTest(
       verbose, testForGroupPlaying:true, thisInstance:thisInstance);
  }

  isGroupPlaying  { |verbose|  ^this.class.isGroupPlaying(verbose, this); }


  //
  *isGroupRunning  { |verbose, thisInstance|
    ^this.isGroupActiveTest(
       verbose, testForGroupPlaying:false, thisInstance:thisInstance);
  }

  isGroupRunning  { |verbose|  ^this.class.isGroupRunning(verbose, this); }




  //---------------------------------------------------------- -o--
  // Helper methods.

  //------------------------ -o-
  *isGroupActiveTest  { | verbose, testForGroupPlaying,
                          thisInstance
                        |
    //DEFAULTS.
    verbose              = verbose              ?? true;
    testForGroupPlaying  = testForGroupPlaying  ?? true;


    //
    if (thisInstance.notNil, {
      if (this.hasInstanceVariable(\group).not, 
      { 
        if (verbose, {
          Log.error( thisFunction, 
                     "Class % has NO INSTANCE VARIABLE named group.", 
                        this.classname
                   ).postln; 
        });

        ^false; 
      });

      if (testForGroupPlaying, {
        ^Node.isPlaying(thisInstance.group); 
      }, {
        ^Node.isRunning(thisInstance.group); 
      });

    }); //thisInstance.notNil


    //
    if (this.hasClassVariable(\group).not, 
    { 
      if (verbose, {
        Log.error( thisFunction, 
                   "Class % has NO CLASS VARIABLE named group.", this.classname
                 ).postln; 
      });

      ^false; 
    });

    if (testForGroupPlaying, {
      ^Node.isPlaying(this.group); 
    }, {
      ^Node.isRunning(this.group); 
    });
  }




  //---------------------------------------------------------- -o--
  // MOS protocol methods.

  //------------------------ -o-
  *pretty  { | pattern, elisionLength, depth,
               casei, compileString, indent, initialCallee, 
               enumerate, bulletIndex, enumerationOffset,
               minimumSpacerSize, nolog, shortVersion
             |
    var  title  = this.classname.toUpper;

    var  maxKeyLength  = "appBrowserDefault".size;

    var  enviroA1, enviroA2, 
         enviroB, enviroC, enviroD,
         enviroArray;


    //
    enviroA1 = (
        appBrowser:             this.appBrowser,
        //appBrowserPreference:   this.appBrowserPreference,
    );
    enviroA2 = (
        appBrowserDefault:      this.appBrowserDefault,
        appTextEdit:            this.appTextEdit,
    );

        //SPACER
              
    enviroB = (
        baseDirname:            this.baseDirname,
        workingDirectory:       this.workingDirectory,
    );

        //SPACER


    enviroD = (
        group:                  group.pretty(nolog:true)
                                        .minel(elisionLength:128),
    );


    enviroArray = [ enviroA1, enviroA2, (), 
                    enviroB, (),
                    //enviroC, (),
                    enviroD,
                  ];


    //
    ^this.new.prettyLocal( 
            enviroArray, title,
            pattern, elisionLength, depth,
            casei, compileString, indent, initialCallee,
            enumerate, bulletIndex, enumerationOffset, 
            minimumSpacerSize:maxKeyLength, 
              nolog:nolog, shortVersion:shortVersion
          );
  }


}  // MobileSound

