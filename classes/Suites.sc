//
// Suites.sc
//
// A subclass of MobileSound to contain (and distinguish) performance
// oriented classes from core MobileSound utility and support classes.
//
//
//
// PUBLIC CLASS VARIABLES--
//   workingDirectory
//   group
//
//
// MOS PROTOCOL SUPPORT--
//   (*)mosVersion
//   *pretty
//
//
// DEPENDENCIES--
//   File_MOS
//   Object_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------

Suites : MobileSound
{
  classvar     classVersion = "0.1";   //RELEASE

  classvar  <  baseDirectory,
            <  workingDirectory;

  classvar  <> group;




  //---------------------------------------------------------- -o--
  *mosVersion { ^super.mosVersion(classVersion); }
  mosVersion  { ^this.class.mosVersion; }




  //---------------------------------------------------------- -o--
  // Constructors, state.

  //------------------------ -o-
  // ASSUME  Server.default represents Server characteristics for
  //         all server instances.
  //
  *initClass  
  {
    Class.initClassTree(Server);

    group = nil;

    baseDirectory     = this.classname.toLower;
    workingDirectory  = super.workingDirectory +/+ baseDirectory;


    //
    StartUp.add  {
      File.createDirectory(workingDirectory);  //XXX  Best effort!
    }

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

    var  maxKeyLength = "workingDirectory".size;

    var  enviroA, enviroB, 
         enviroArray;  // ADD


    //
    enviroA = (
      workingDirectory:         workingDirectory,
    );

    enviroB = (
      group:                    group.pretty(nolog:true).minel(elisionLength:128),
    );

    enviroArray = [ enviroA, 
                    enviroB, 
                  ];


    //
    ^this.prettyLocal( 
            enviroArray, title,
            pattern, elisionLength, depth,
            casei, compileString, indent, initialCallee,
            enumerate, bulletIndex, enumerationOffset, 
            minimumSpacerSize:maxKeyLength, 
              nolog:nolog, shortVersion:shortVersion
          );
  }


}  //Suites

