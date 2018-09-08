//
// Z.sc
//
// Utility methods.  
// (Anything that does not merit a separate class or extension.)
//
//
// CLASS METHODS--
//   *versionSC  *versionApple
//
//   *ideType  
//     *isIDEQt  *isIDEVim  
//   *systemType
//     *isSystemOSX  *isSystemIOS
//
//   *recompile
//
//   *openURL  
//   *openTextEdit  *openTerminal
//   *open
//   *openStartupFiles
//
//   *helpFolderURL  
//     *helpBrowse     *helpClasses    *helpGuides     
//     *helpHelp       *helpOther      *helpOverview   
//     *helpReference  *helpSearch     *helpTutorial   
//   *helpClassTree
//   *helpGenerateURL  
//   *moshelp  
//
//   *helpGUI
//   *helpGUIClass
//   *helpGUISynthDef
//
//   *dashboardSystem
//   *dashboardBrowseObjects
//
//   *separatorSpace  *separatorLine  *sep  [*seps  *sepl] 
//   *separatorBegin  *separatorEnd         [*sepb  *sepe]
//   *dots
//
//   *mouseXY
//
//   *secondsToFrames  [*s2f]
//   *framesToSeconds  [*f2s]
//
//   *roundSecondsToBlockSampleSize
//   *unixCmd
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Log
//   Object_MOS
//   String_MOS
//   Window_MOS
//
//
// OSX  Paths are currently OSX-centric.
//
//
// NB  Returning the result of *.unixCmd includes the process ID
//     followed by the string "RESULT = ...".
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------

Z : MobileSound
{
  classvar   classVersion = "0.5";   //RELEASE




  //--------------------------------------------------------------- -o--
  // Lifecycle, reflection, class identity.

  *mosVersion { ^super.mosVersion(classVersion); }

  *versionSC  { ^Main.version; }

  *versionApple { 
    if (Z.isSystemOSX.not && Z.isSystemIOS.not, {
      ^Log.error(thisFunction, "Only supported on Apple platforms.");
    });

    ^"sw_vers -productVersion".unixCmdGetStdOut.stripWhiteSpace; 
  }




  //--------------------------------------------------------------- -o--
  // Class methods.

  //------------------------ -o-
  // Information about local system/environment.

  *ideType   { ^Platform.ideName.asSymbol; }
  *isIDEQt   { ^(\scqt == Z.ideType); }
  *isIDEVim  { ^(\scvim == Z.ideType); }


  //
  *systemType  { ^Platform.platformDir.asSymbol; }

  *isSystemOSX  { ^(\osx == Z.systemType); }
  *isSystemIOS  { ^(\iphone == Z.systemType); }



  //------------------------ -o-
  // VS  Platform.new.loadStartupFiles 
  // VS  thisProcess.recompile
  // VS  thisProcess.startup
  //
  *recompile  { 
    thisProcess.stop; 
    Platform.new.recompile; 
  }     



  //------------------------ -o-
  *openURL  { |url, useSSL|             

    var  httpPrefix   = "http://",
         httpsPrefix  = "https://",
         cmdOpen      = "open -a",
         cmd          = "";


    //DEFAULTS.
    useSSL = useSSL ?? false;


    //
    if (url.isAbsolutePath.not && url.regexpMatch("^[a-z]+:\/\/").not, {
      var  absoluteFilePath  = PathName.new(url).absolutePath;

      if (absoluteFilePath.pathExists, {
        url = absoluteFilePath;

      }, {
        if (useSSL, {
          url = httpsPrefix ++ url;
        }, {
          url = httpPrefix ++ url;
        });

      }); //absoluteFilePath.pathExists
    });


    //
    cmd = ("% % %").format(cmdOpen, MobileSound.appBrowser, url.shellQuote);

    ^cmd.unixCmd;
  }


  //------------------------ -o-
  *openTextEdit  { |file|
    if (Z.isSystemOSX.not, {
      ^Log.error(thisFunction, "Only supported on OSX.");
    });

    ^("open -e " ++ file.standardizePath.quote).unixCmd;
  }



  //------------------------ -o-
  // ASSUME  "scvim" is in PATH.
  //
  *openTerminal  { | filepath, filepathAsClass                   //FORK
                   |
    var  fileobj,
         chmod,
         oitScript,
         oitScriptFile;

    //
    if (Z.isSystemOSX.not, {
      ^Log.error(thisFunction, "Only supported on OSX.");
    });


    //DEFAULTS.
    filepathAsClass = filepathAsClass ?? false;

    if (filepathAsClass, { 
      filepath = filepath.classname.asSymbol.asClass.filename; 
    });


    //
    oitScriptFile  = File.pathToUserTmpFile("sc-oit", suffix:"sh");
    chmod          = ("chmod a+x %").format(oitScriptFile);

    oitScript =    "#!/bin/bash\n"
                ++ "export VISUAL=scvim\n"
                ++ ("exec less %").format(filepath.standardizePath.quote);

    fileobj = File.open(oitScriptFile, "w");
    fileobj.write(oitScript);
    fileobj.flush;
    fileobj.close;

    chmod.unixCmd;


    //
    {
      ("open -b com.apple.terminal " ++ oitScriptFile).unixCmd;
      1.wait;
      oitScriptFile.delete(verbose:false);
    }.fork;

    ^'';
  }


  //------------------------ -o-
  // open stuff...
  //
  *open  {  |file, folder|
    var  cmd,
         folderOption  = "-R";
    
    //DEFAULTS.
    folder = folder ?? false;

    if (folder.not, { folderOption = ""; });

    //
    cmd = ("open % %").format(folderOption, file.standardizePath.shellQuote);

    ^cmd.unixCmd;
  }


  //------------------------ -o-
  *openStartupFiles  {
    ^Platform.new.startupFiles.do({ |elem| Z.openTerminal(elem); });
  }




  //----------------------- -o-
  // NB  Adding URL arguements requires local server.  
  //     (eg: "Search.html#Object")
  //
  *helpFolderURL  { | pattern
                    |
    var  helpURL  = Platform.userAppSupportDir +/+ "Help/";

    var  patternRegexp;

    var  arrayOfMatches,
         rval  = "";

    //
    pattern       = if (pattern.notNil, { pattern.asString; }, { "search"; });
    patternRegexp = ("^%").format(pattern);

    arrayOfMatches = 
      PathName.new(helpURL).entries.select({ | elem
                                             |
             elem.fullPath.basename.regexpMatch("^[A-Z]", casei:false)
          && elem.fullPath.basename.regexpMatch(patternRegexp);
      });


    //
    if (arrayOfMatches.size <= 0, {
      ^Log.error(thisFunction, 
             "<pattern> DOES NOT MATCH any help folder item.  (%)", pattern); 
    });

    if (arrayOfMatches.size > 1, {
      rval = Log.error(thisFunction, 
               "<pattern> MATCHES MULTIPLE folder items.  (%)", pattern); 

      arrayOfMatches.do({ |elem, index|
        rval = ("%\n  %:  %").format(rval, index, elem.fullPath);
      });

      ^("\n%\n").format(rval);
    });


    //
    ^Z.openURL(arrayOfMatches[0].fullPath);
  }


  //
  *helpBrowse     { ^Z.helpFolderURL(\Browse); }
  *helpClasses    { ^Z.helpFolderURL(\Classes); }
  *helpGuides     { ^Z.helpFolderURL(\Guides); }
  *helpHelp       { ^Z.helpFolderURL(\Help); }
  *helpOther      { ^Z.helpFolderURL(\Other); }
  *helpOverview   { ^Z.helpFolderURL(\Overview); }
  *helpReference  { ^Z.helpFolderURL(\Reference); }
  *helpSearch     { ^Z.helpFolderURL(\Search); }
  *helpTutorial   { ^Z.helpFolderURL(\Tutorial); }

  *helpClassTree  { 
    ^Z.openURL(Platform.userAppSupportDir +/+ "Help/Overviews/ClassTree.html")
  }



  //----------------------- -o-
  // INPUTS--
  //   pattern         Pattern for classname or to search in SC document list.
  //   index           Integer index into list of matches against pattern.
  //   patternAsClass  Take pattern as literal classname, instead of
  //                     pattern to match against SC document list.
  // OUTPUTS--
  //   Open specific help file,
  //     or search window on all SC help files,
  //     or generates indexed list of matches against SC document list,
  //     or error.
  //
  // NB  URL "http://localhost/.../Search.html#pattern" requires local server.
  //
  *helpGenerateURL  { | pattern, index, patternAsClass
                      |
    var  schelpFile      = nil,
         htmlFile        = nil,
         htmlFileExists,
         documentKey     = nil,
         ignorePattern   = "/ignore/",
         hashPosition,
         parseAndRender  = false;

    var  rval;


    //DEFAULTS.
    pattern         = if (pattern.isNil, { ""; }, 
                                         { pattern.classname.asString; } 
                         );
    patternAsClass  = patternAsClass ?? false;

    
    //
    if (pattern.size <= 0, {
      ^Z.helpSearch;
    });


    // Generate schelpFile and htmlFile.
    //
    if (patternAsClass, {
      if (pattern.asSymbol.isClassName.not, {
        ^Log.error(thisFunction, 
                     "<pattern> is not a well-known class.  (%)", pattern); 
      });

      //pattern = pattern.classname.asSymbol.asClass;

      //
      htmlFile = OSXPlatform.new.findHelpFile(pattern).replace("file://", "");

      hashPosition = htmlFile.findBackwards("#");
      if (hashPosition.notNil, { htmlFile = htmlFile[0..(hashPosition-1)]; });

      schelpFile = 
        File.findFilesUnderArrayOfDirectoriesWithSuffixPattern(
            arrayOfDirectories:  SCDoc.helpSourceDirs, 
            suffix:              \schelp,
            pattern:             "/" ++ pattern ++ "$", 
            casei:               false,
            returnFullPath:      true,
          );

      documentKey = ("Classes/%").format(pattern);


      //
      if (schelpFile.size != 1, { 
        schelpFile = nil; 

        if (schelpFile.size > 1, {
          ^Log.error(
             thisFunction, 
             "<pattern> matches more than one class schelp file.  (%)", 
             pattern
           );
        });
      });

      if (schelpFile.size > 0, {
        schelpFile = schelpFile[0];

        if (schelpFile.notNil && (schelpFile.size > 0), {
          if (schelpFile.regexpMatch(ignorePattern, casei:false), {
            schelpFile = nil; 
          });
        });
      });


    //else -- patternAsClass.not
    }, {  
      var  documentMatches = 
             SCDoc.documents.keys.asList.sort.select({ |elem|
               elem.regexpMatch(pattern)
                        && elem.regexpMatch(ignorePattern, casei:false).not
             });

      if (documentMatches.size <= 0, {
        Log.error(thisFunction, "No match for \"%\".", pattern).postln;
        ^Z.helpSearch;
      });

      if ((documentMatches.size > 1) && index.isNil, {
        Log.warning(
          thisFunction, 
          "Found % entries for pattern \"%\".", documentMatches.size, pattern
        ).postln; 

        rval = "";
        documentMatches.asList.sort.do({ |elem, index|  
          rval = rval ++ ("  %: %\n").format(index, elem);
        });

        ^rval;
      });

      index = index ?? 0;

      documentKey  = documentMatches.asArray[index];
      schelpFile   = SCDoc.documents[documentKey].fullPath;

      htmlFile     = ("%/Help/%.html").format( 
                                         Platform.userAppSupportDir,
                                         documentKey.asString
                                       );
    }); //endif -- patternAsClass



    // Logic for opening and rebuilding help system files.
    //
    // ASSUME...
    //   . schelpFile may be nil, but if defined it exists.
    //   . htmlFile may be nil, but if defined it may NOT exist.
    //
    htmlFileExists = if (htmlFile.notNil, { htmlFile.pathExists; }, { false; });


    //
    if (htmlFileExists.not, {
      SCDoc.verbosity = 0;
      SCDoc.renderAll;

      if (htmlFile.pathExists.not, {
        ^Log.error(
           thisFunction, 
           "HTML file still does NOT exist after SCDoc.renderAll!  (%)",
           htmlFile
         ); 
      });

    }, {
      if (schelpFile.notNil, {
        if (File.isFileNewer(schelpFile, htmlFile), { parseAndRender = true; });
      });

      if (patternAsClass, {
        var  mosFilename  = pattern.mosFilename;

        if (mosFilename.notNil, {
          if (File.isFileNewer(mosFilename, htmlFile), {
            parseAndRender = true; 
          });
        });

        if (File.isFileNewer(pattern.asSymbol.asClass.filename, htmlFile), {
          parseAndRender = true;
        });
      });


      //
      if (parseAndRender, {
        //htmlFile.delete(verbose:false);
        htmlFile.delete;

        SCDoc.verbosity = 3;
        SCDoc.parseAndRender(SCDoc.documents[documentKey]);
      });
    });


    //
    ^Z.openURL(htmlFile);

  } // *helpGenerateURL



  //----------------------- -o-
  *moshelp  { | pattern, index, patternAsClass, folders
              |

    //DEFAULTS.
    folders = folders ?? false;

    if (folders, { 
      ^Z.helpFolderURL(pattern); 
    }, {
      ^Z.helpGenerateURL(pattern, index, patternAsClass);
    });
  }  



  //------------------------ -o-
  *helpGUI  { ^Help.new.gui; }

  *helpGUIClass  { ^Object.browse; }    // VS  thisProcess.showClassBrowser

  *helpGUISynthDef  { ^SynthDescLib.global.browse; }



  //------------------------ -o-
  *dashboardSystem  { | server
                      |
    var  meterWindowName    = "^.*levels.*$",
         meterWindowHeight  = 0,
         meterWindowRect,
         meterWindow;

    var  channelWindowName             = "Stethoscope",
         channelWindowVerticalOffset   = 50,
         channelWindowHeight           = 700,  //XXX
         channelWindowChannelCount,
         channelWindowChannelCountMin  = 24,
         channelWindowYZoom            = 16,
         channelWindowRect,
         channelWindow;

    var  frequencyScopeWindow,
         frequencyScopeWindowHeight;

    var  serverStatusWindowName            = "server$",
         serverStatusWindowVerticalOffset  = 50,
         serverStatusWindowRect,
         serverStatusWindow;


    //DEFAULTS.
    server = server ?? Server.default;

    channelWindowChannelCount =   server.options.numInputBusChannels 
                                + server.options.numOutputBusChannels 
                                + server.options.numInputBusChannels
                                    .max(server.options.numOutputBusChannels);

    if (channelWindowChannelCount < channelWindowChannelCountMin,  { 
      channelWindowChannelCount = channelWindowChannelCountMin;
    });


    //
    meterWindow = Window.findWindowByName(meterWindowName);

    if (meterWindow.notNil, {
      meterWindow.front;
    }, {
      Server.channelLevels;
      meterWindow = Window.findWindowByName(meterWindowName);
    });

    if (meterWindow.notNil, {
      meterWindowRect  = 
          Rect( Window.offsetFromDisplayRight(meterWindow.bounds.width), 
                Window.offsetFromDisplayTop, 
                meterWindow.bounds.width, 
                meterWindow.bounds.height
              );

      meterWindow.bounds = meterWindowRect;

      //
      AppClock.sched(0.0, { |appClockTime| 
        meterWindow.bounds = meterWindowRect;
      });
    });


    //
    channelWindow = Window.findWindowByName(channelWindowName);

    if (channelWindow.notNil, {
      channelWindow.front;

    }, {
      Server.channelScope;

      Server.channelScope.yZoom        = channelWindowYZoom;
      Server.channelScope.numChannels  = channelWindowChannelCount;

      channelWindow = Window.findWindowByName(channelWindowName);
    });

    if (channelWindow.notNil, 
    {
      channelWindow.bounds = Rect( channelWindow.bounds.left, 
                                   channelWindow.bounds.right, 
                                   channelWindow.bounds.width, 
                                   channelWindowHeight
                                 );
      
      if (meterWindow.notNil, { 
        meterWindowHeight = meterWindow.bounds.height; 
      }, {
        meterWindowHeight = 0;
        channelWindowVerticalOffset = 0;
      });

      channelWindowRect = 
          Rect( Window.offsetFromDisplayRight(channelWindow.bounds.width),
                Window.offsetFromDisplayTop(
                  meterWindow.bounds.height + channelWindow.bounds.height 
                    + channelWindowVerticalOffset),
                channelWindow.bounds.width,
                channelWindow.bounds.height
              );

      channelWindow.bounds = channelWindowRect;
    });


    //
    frequencyScopeWindow = Server.frequencyScope;

    serverStatusWindow = Window.findWindowByName(serverStatusWindowName);

    if (serverStatusWindow.notNil, {
      serverStatusWindow.front;
    }, {
      Server.windowgui;
      serverStatusWindow = Window.findWindowByName(serverStatusWindowName);
    });

    if (serverStatusWindow.notNil, 
    {
      if (frequencyScopeWindow.notNil, { 
        frequencyScopeWindowHeight = frequencyScopeWindow.bounds.height; 
      }, {
        frequencyScopeWindowHeight = 0;
        serverStatusWindowVerticalOffset = 0;
      });

      serverStatusWindowRect = 
          Rect( 0,
                Window.offsetFromDisplayTop(
                  frequencyScopeWindow.bounds.height 
                    + serverStatusWindow.bounds.height 
                    + serverStatusWindowVerticalOffset),
                serverStatusWindow.bounds.width,
                serverStatusWindow.bounds.height
              );

      serverStatusWindow.bounds = serverStatusWindowRect;
    });


    //
    Server.nodesgui;

    ^'';
  }

  //
  *dashboardBrowseObjects  { 
    Z.helpGUI;
    Z.helpGUIClass;
    Z.helpGUISynthDef;

    ^'';
  }



  //------------------------ -o-
  *separatorSpace  {
    var  str = ("\n\n");
    ^str;
  }

  *separatorLine  { |title=""|
     var  terminalWidth  = (80 - 2), 
          lineLength     = 0,
          lineHook       = " -o--  ",
          lineCharacter  = "-",
          str            = nil;

     //
     if (title.isNil,        { title = ""; });
     if (title.isString.not, { title = title.asString; });

     //
     lineLength = (terminalWidth - lineHook.size - title.size);

     str = ("%%%").format(
               lineCharacter.dup(lineLength).join, lineHook, title.toUpper );
     ^str;
  }

  *sep  { |title|  
    ^("%%%").format( 
         this.separatorSpace, this.separatorLine(title), this.separatorSpace);
  }


  //
  *seps  { ^this.separatorSpace; }                 // ALIAS
  *sepl  { |title|  ^this.separatorLine(title); }  // ALIAS


  //
  *separatorBegin  { |title=""|  
    ^(this.seps ++ this.separatorLine(title ++ " BEGIN")); 
  }
  *separatorEnd    { |title=""|  
    ^(this.separatorLine(title ++ " END  ") ++ this.seps); 
  }

  *sepb  { |title|  ^this.separatorBegin(title); }  // ALIAS
  *sepe  { |title|  ^this.separatorEnd(title); }    // ALIAS



  //
  *dots  { |obj, length=17|
    ^("%%").format(".".dup(length).join, obj ?? "");
  }



  //------------------------ -o-
  *mouseXY  { ^Platform.getMouseCoords; }



  //------------------------ -o-
  // NB  Allow seconds to exceed length of buffer.
  //
  *secondsToFrames  { | second, totalSeconds, sampleRate, logContext
                      |  
    var  specificSecond  = second ?? totalSeconds;


    //DEFAULTS.
    if (logContext.isNil, { logContext = thisFunction; });

    //
    if (Parse.isInstanceOfClass(logContext, 
                  specificSecond, "second", Number).not, { ^''; });

    if (Parse.isInstanceOfClass(logContext, 
                  sampleRate, "sampleRate", Number).not, { ^''; });


    //
    if ((specificSecond < 0) || (sampleRate < 0), 
    {
      Log.error( logContext, 
                 "second and sampleRate MUST BE GREATER THAN ZERO.  "
                    ++ "(second %  sampleRate %)", specificSecond, sampleRate
               ).postln; 
      ^''; 
    });

    //
    //^((specificSecond * sampleRate).round);   //NB  Frames may be fractional!
    ^(specificSecond * sampleRate); 
  }

  // NB  DO NOT allow frames to exceed size of buffer.
  //
  *framesToSeconds  { | frame, totalFrames, sampleRate, logContext
                      |   
    var  specificFrame  = frame ?? totalFrames;


    //DEFAULTS.
    if (logContext.isNil, { logContext = thisFunction; });

    //
    if (Parse.isInstanceOfClass(logContext, 
                  specificFrame, "frame", Number).not, { ^''; });

    if (Parse.isInstanceOfClass(logContext, 
                  sampleRate, "sampleRate", Number).not, { ^''; });


    //
    if ((specificFrame < 0) || (sampleRate < 0), 
    {
      Log.error( logContext, 
                 "frame and sampleRate MUST BE GREATER THAN ZERO.  "
                    ++ "(frame %  sampleRate %)", specificFrame, sampleRate
               ).postln; 
      ^''; 
    });

    //
    ^(specificFrame / sampleRate); 
  }


  //ALIAS
  *s2f  { |second, totalSeconds, sampleRate|
    ^this.secondsToFrames(second, totalSeconds, sampleRate); 
  }  

  *f2s  { |frame, totalFrames, sampleRate|
    ^this.framesToSeconds(frame, totalFrames, sampleRate); 
  }  


  //------------------------ -o-
  //UNUSED.
  //
  *roundSecondsToBlockSampleSize  { | seconds, server, logContext
                                    |
    var  blockSize,
         blockSampleSize;


    //DEFAULTS.
    server = server ?? Server.default;

    if (logContext.isNil, { logContext = thisFunction; });

    //
    if (Parse.isInstanceOfClass(logContext, 
                  seconds, "seconds", SimpleNumber).not, { ^''; });


    //
    blockSize        = server.options.blockSize;
    blockSampleSize  = (seconds * server.sampleRate).round(blockSize);

    ^blockSampleSize;
  }


  //------------------------ -o-
  *unixCmd  { | unixCommand
              |
    var  unixCommandWithPadding;

    //
    if (Parse.isInstanceOfClass(thisFunction, 
          unixCommand, "unixCommand", [String, Symbol]).not, { ^nil; });

    //
    unixCommand             = unixCommand.asString;
    unixCommandWithPadding  = ("% | sed 's/^/  /'").format(unixCommand);

    ^Log.msg("UNIX COMMAND %\n\n  %", 
                unixCommand, unixCommandWithPadding.unixCmdStdOut); 
  }

}  // Z




                                /* 
        generateURL common error...
        (always) works second time...

        SCDoc.parseFile*
        ================================================

02:12:24  INFO  String.delete(:) -- DELETED object at path.  (/Users/evans/Library/Application Support/SuperCollider/Help/Classes/ScaleAD.html)
ERROR: Primitive '_SCDoc_ParseFile' failed.
Wrong type.
RECEIVER:
class SCDoc (0x109d74c40) {
  instance variables [19]
    name : Symbol 'SCDoc'
    nextclass : instance of Meta_SCDocEntry (0x105b13a40, size=19, set=5)
    superclass : Symbol 'Object'
    subclasses : nil
    methods : nil
    instVarNames : nil
    classVarNames : instance of SymbolArray (0x109d74dc0, size=8, set=2)
    iprototype : nil
    cprototype : instance of Array (0x109d74e80, size=8, set=3)
    constNames : nil
    constValues : nil
    instanceFormat : Integer 0
    instanceFlags : Integer 0
    classIndex : Integer 227
    classFlags : Integer 0
    maxSubclassIndex : Integer 227
    filenameSymbol : Symbol '/Users/evans/user/project/composition/ea/sc/app/SuperCollider-v3.6.6/SuperCollider.app/Contents/Resources/SCClassLibrary/SCDoc/SCDoc.sc'
    charPos : Integer 11987
    classVarIndex : Integer 210
}
CALL STACK:
        MethodError:reportError   0x10f1cc508
                arg this = <instance of PrimitiveFailedError>
        Nil:handleError   0x10f1c6ba8
                arg this = nil
                arg error = <instance of PrimitiveFailedError>
        Thread:handleError   0x10f7f4ad8
                arg this = <instance of Thread>
                arg error = <instance of PrimitiveFailedError>
        Object:throw   0x10f1ca8d8
                arg this = <instance of PrimitiveFailedError>
        Object:primitiveFailed   0x10f24ab08
                arg this = <instance of Meta_SCDoc>
        Meta_SCDoc:parseDoc   0x110919c28
                arg this = <instance of Meta_SCDoc>
                arg doc = <instance of SCDocEntry>
                var add = nil
                var root = nil
        Meta_SCDoc:parseAndRender   0x11160fc58
                arg this = <instance of Meta_SCDoc>
                arg doc = <instance of SCDocEntry>
                var dest = "/Users/evans/Library/Applica..."
                var root = nil
        Meta_Z:helpGenerateURL   0x111516b48
                arg this = <instance of Meta_Z>
                arg pattern = "ScaleAD"
                arg index = nil
                arg patternAsClass = true
                var schelpFile = nil
                var htmlFile = "/Users/evans/Library/Applica..."
                var htmlFileExists = true
                var documentKey = "Classes/ScaleAD"
                var ignorePattern = "/ignore/"
                var hashPosition = nil
                var parseAndRender = true
                var rval = nil
        Interpreter:interpretPrintCmdLine   0x110d7cdc8
                arg this = <instance of Interpreter>
                var res = nil
                var func = <instance of Function>
                var code = "
Z.helpGenerateURL(ScaleAD, ..."
                var doc = nil
                var ideClass = nil
        Process:interpretPrintCmdLine   0x111558918
                arg this = <instance of Main>
^^ The preceding error dump is for ERROR: Primitive '_SCDoc_ParseFile' failed.
Wrong type.



        =====================================================
        ANOTHER ONE
        =====================================================
a Main
02:25:48  INFO  String.delete(:) -- DELETED object at path.  (/Users/evans/Library/Application Support/SuperCollider/Help/Classes/AbstractGroup.html)
ERROR: Primitive '_SCDoc_ParseFile' failed.
Wrong type.
RECEIVER:
class SCDoc (0x10f615c40) {
  instance variables [19]
    name : Symbol 'SCDoc'
    nextclass : instance of Meta_SCDocEntry (0x10efc4a40, size=19, set=5)
    superclass : Symbol 'Object'
    subclasses : nil
    methods : nil
    instVarNames : nil
    classVarNames : instance of SymbolArray (0x10f615dc0, size=8, set=2)
    iprototype : nil
    cprototype : instance of Array (0x10f615e80, size=8, set=3)
    constNames : nil
    constValues : nil
    instanceFormat : Integer 0
    instanceFlags : Integer 0
    classIndex : Integer 227
    classFlags : Integer 0
    maxSubclassIndex : Integer 227
    filenameSymbol : Symbol '/Users/evans/user/project/composition/ea/sc/app/SuperCollider-v3.6.6/SuperCollider.app/Contents/Resources/SCClassLibrary/SCDoc/SCDoc.sc'
    charPos : Integer 11987
    classVarIndex : Integer 210
}
CALL STACK:
        MethodError:reportError   0x11070a178
                arg this = <instance of PrimitiveFailedError>
        Nil:handleError   0x1102379a8
                arg this = nil
                arg error = <instance of PrimitiveFailedError>
        Thread:handleError   0x112b42628
                arg this = <instance of Thread>
                arg error = <instance of PrimitiveFailedError>
        Object:throw   0x113356558
                arg this = <instance of PrimitiveFailedError>
        Object:primitiveFailed   0x1136b6068
                arg this = <instance of Meta_SCDoc>
        Meta_SCDoc:parseDoc   0x11342ee18
                arg this = <instance of Meta_SCDoc>
                arg doc = <instance of SCDocEntry>
                var add = nil
                var root = nil
        Meta_SCDoc:parseAndRender   0x11352f978
                arg this = <instance of Meta_SCDoc>
                arg doc = <instance of SCDocEntry>
                var dest = "/Users/evans/Library/Applica..."
                var root = nil
        Meta_Z:helpGenerateURL   0x112e94c78
                arg this = <instance of Meta_Z>
                arg pattern = "AbstractGroup"
                arg index = nil
                arg patternAsClass = true
                var schelpFile = nil
                var htmlFile = "/Users/evans/Library/Applica..."
                var htmlFileExists = true
                var documentKey = "Classes/AbstractGroup"
                var ignorePattern = "/ignore/"
                var hashPosition = nil
                var parseAndRender = true
                var rval = nil
        Interpreter:interpretPrintCmdLine   0x112c0bbe8
                arg this = <instance of Interpreter>
                var res = nil
                var func = <instance of Function>
                var code = "
Z.helpGenerateURL(AbstractG..."
                var doc = nil
                var ideClass = nil
        Process:interpretPrintCmdLine   0x1125277e8
                arg this = <instance of Main>
^^ The preceding error dump is for ERROR: Primitive '_SCDoc_ParseFile' failed.
Wrong type.

                                */



