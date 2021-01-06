//
// Platform_MOS.sc
//
//
// PUBLIC METHODS--
//   userDocuments
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//   pretty
//
//
// MOS DEPENDENCIES--
//   File_MOS
//   Object_MOS
//   String_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Platform  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.2";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Class methods.

  *userDocuments  {
    var  userDocumentsPath  = "~/SuperCollider/Documents".standardizePath();

    if (File.createDirectory(userDocumentsPath), {
      ^userDocumentsPath;
    }, {
      ^nil;
    });
  }




  //------------------------------------------- -o--
  // MOS protocol methods.

  //------------------------ -o-
  *pretty  { | pattern, elisionLength, depth,
               casei, compileString, indent, initialCallee, 
               enumerate, bulletIndex, enumerationOffset,
               minimumSpacerSize, nolog, shortVersion
             |

    var  title  = this.classname.toUpper;

    var  maxKeyLength  = "systemAppSupportDir".size;

    var  enviroArray,
         enviroA1, enviroA2, enviroA3, enviroA4, enviroA5,
         enviroB, enviroC, 
         enviroD1, enviroD2, enviroD3, enviroD4,
         enviroE,
         enviroF1, enviroF2, 
         enviroG,
         enviroH, 
         enviroM,
         rval;


    var  platformInstance  = thisProcess.platform;

    var  elidePath;


    //
    elidePath = { | pathThatIsTooLong, containedPath, sourceClass
                  |
      sourceClass = sourceClass ?? Platform;

      ("(%)%").format(
                 containedPath,
                 ("%.%.replace(Platform.%, "")").format( sourceClass.classname,
                                                         pathThatIsTooLong, 
                                                         containedPath
                                                 ).compile.();
               );
    }; //elidePath


    //
    enviroA1 = (
      versionSC: 	Z.versionSC, 
    );

    enviroA2 = (
      systemType: 	Z.systemType,
    );

    enviroA3 = case
      //
      { Z.isSystemOSX; }
        {(
          versionApple:  	Z.versionApple,
        )}

      //
      { Z.isiPhone; }
        {(
          iosVersion:  	Z.versionApple,
        )}
      
    ; //endcase

    enviroA4 = (
      ideType: 	        Z.ideType,
    );

    enviroA5 = (
      defaultGUIScheme: platformInstance.defaultGUIScheme,
    );

        // SPACER

    enviroB = (
      hasMobileSound:    platformInstance.hasFeature(\MobileSound)
    );

        // SPACER

    enviroC = (
      systemAppSupportDir:      Platform.systemAppSupportDir,
      systemExtensionDir:
      		elidePath.("systemExtensionDir", "systemAppSupportDir"),
    );

        // SPACER

    enviroD1 = (
      userAppSupportDir:        Platform.userAppSupportDir,
    );

    enviroD2 = (
      userConfigDir:     elidePath.("userConfigDir", "userAppSupportDir"),
      userExtensionDir:  elidePath.("userExtensionDir", "userAppSupportDir"),
    );

    enviroD3 = (
      synthDefDir:  elidePath.("synthDefDir", "userAppSupportDir", SynthDef),
    );

        // SPACER

    enviroE = (
      defaultTempDir: 	Platform.defaultTempDir,
      recordingsDir:    platformInstance.recordingsDir ?? "(unset)",
      userDocuments:    this.userDocuments(),
    );

        // SPACER

    enviroF1 = (
      resourceDir: 	Platform.resourceDir,
    );

    enviroF2 = (
      classLibraryDir:  elidePath.("classLibraryDir", "resourceDir"),
      helpDir: 	        elidePath.("helpDir", "resourceDir"),
    );

        // SPACER

    enviroG = (
      startupFiles: 	platformInstance.startupFiles,
    );

        // SPACER

    rval = ServerBoot.objects ?? "(unset)";  // XXXFIX  Broken by .pretty...
          
    enviroM = (
      \doOnCmdPeriod:           CmdPeriod.objects ?? "(unset)",
      \doOnOnError:             OnError.objects ?? "(unset)",
      \doOnServerBoot:          rval.asString,
      \doOnServerTree:          ServerTree.objects ?? "(unset)",
      \doOnServerQuit:          ServerQuit.objects ?? "(unset)",
      \doOnShutDown:            ShutDown.objects ?? "(unset)",
      \doOnStartUp:             StartUp.objects ?? "(unset)",
    );


    //
    enviroArray = [     enviroA1, enviroA2, enviroA3, enviroA4, enviroA5, (), 
                        enviroB, (),
                        enviroC, (),
                        enviroD1, enviroD2, enviroD3, enviroD4, (), 
                        enviroE, (),
                        enviroF1, enviroF2, (),
                        enviroG, (),
                        //(), enviroM
                  ];

    ^this.prettyLocal( 
            enviroArray, title,
            pattern, elisionLength, depth,
            casei, compileString, indent, initialCallee, 
            enumerate, bulletIndex, enumerationOffset, 
            minimumSpacerSize:maxKeyLength, 
              nolog:false, shortVersion:shortVersion
          );
  }


  //ALIAS
  pretty  { | pattern, elisionLength, depth,
              casei, compileString, indent, initialCallee, 
              enumerate, bulletIndex, enumerationOffset,
              minimumSpacerSize, nolog, shortVersion
            |
    ^this.class.pretty( 
                  pattern, elisionLength, depth,
                  casei, compileString, indent, initialCallee,
                  enumerate, bulletIndex, enumerationOffset,
                  minimumSpacerSize, nolog, shortVersion
                );
  }


} //Platform

