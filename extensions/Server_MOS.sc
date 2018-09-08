//
// Server_MOS.sc
//
//
// PUBLIC CLASS METHODS--
//   *localhost
//
//   (*)alls
//   (*)allsRunning
//   isRunning
//   (*)panic
//
//   (*)uptime   [(*)up]
//   (*)output 
//
//   (*)nodes  (*)nodesgui
//   (*)channelLevels
//   (*)channelScope
//   (*)frequencyScope
//   (*)windowgui
//
//   (*)muteToggle
//   (*)volumegui
//   (*)dumpToggle
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//   pretty
//
//
// MOS DEPENDENCIES--
//   Log
//   Object_MOS
//   Parse
//   Window
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Server  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.2";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Public methods.

  //------------------------ -o-
  *localhost  { ^Server.local; }


  //------------------------ -o-
  *alls  { ^this.all.asArray.asString; }
  alls   { ^this.class.alls; }     //ALIAS


  *allsRunning  { ^Server.allRunningServers.asArray.asString; }
  allsRunning   { ^this.class.allsRunning; }      //ALIAS


  isRunning  { ^this.serverRunning; }



  //------------------------ -o-
  // VS  Server.quitAll, OR
  //     Server.killAll
  //
  *panic  { |includeRemoteServers|
     includeRemoteServers = includeRemoteServers ?? true;

     if (Parse.isInstanceOfClass(thisFunction, 
           includeRemoteServers, "includeRemoteServers", Boolean).not, 
                                                        { ^nil; });

     //
     ^Server.freeAll(includeRemoteServers);
  }

  panic  { ^this.freeAll; }  //INSTANCE ONLY



  //------------------------ -o-
  uptime  { |nolog|
    var  str  = "";


    //DEFAULTS.
    nolog = nolog ?? false;

    if (Parse.isInstanceOfClass(thisFunction, 
                  nolog, "nolog", Boolean).not,  { ^nil; });


    //
    if (this.serverRunning.not, {
      str = "(not running)";

    }, {
      str = ("ugens=%  synths=%  groups=%  defs=%  avg/peak=%/%")
        .format(
          this.numUGens, this.numSynths, this.numGroups, this.numSynthDefs,
          if (this.avgCPU.isNil,  { "nil" }, { this.avgCPU.round(0.01) }),
          if (this.peakCPU.isNil, { "nil" }, { this.peakCPU.round(0.01) })
        );
    });


    //
    if (nolog.not, {
      str = ("%:  %").format( 
                    Log.msg("% %", this.classname.toUpper, this.name), 
                    str 
                  );
    });

    ^str;
  }

  //
  up  { |nolog|  ^this.uptime(nolog); }  //ALIAS

  //CLASS
  *uptime  { |nolog|  ^this.default.uptime(nolog); }
  *up      { |nolog|  ^this.uptime(nolog); }



  //------------------------ -o-
  output  { |nolog|
    var  str  = "";


    //DEFAULTS.
    nolog = nolog ?? false;

    if (Parse.isInstanceOfClass(thisFunction, 
                  nolog, "nolog", Boolean).not,  { ^nil; });


    //
    str = ("numChannels=%  lag=%  min/max=%/%  volume=%")
      .format(
        this.volume.numChannels,
        this.volume.lag,
        this.volume.min, this.volume.max,
        this.volume.volume,
      );

    if (this.volume.isMuted, { str = str ++ "  MUTED"; });


    //
    if (nolog.not, {
      str = ("%:  %").format( 
                    Log.msg("% %", this.classname.toUpper, this.name), 
                    str 
                  );
    });

    ^str;
  }

  //
  *output  { |nolog|   ^this.default.output(nolog); }  //CLASS




  //------------------------ -o-
  // XXX  server name from .printOn...
  //
  nodes  { |queryControls|
    var  str  = "";

    //DEFAULTS.
    queryControls = queryControls ?? true;

    if (Parse.isInstanceOfClass(thisFunction, 
          queryControls, "queryControls", Boolean).not,  { ^nil; });


    //
        //str = Log.msg("% %", this.classname.toUpper, this.asString);
    Log.msg("% %", this.classname.toUpper, this.asString).post;  

    if (this.serverRunning, {
                  //str = str ++ //this.queryAllNodesAsString(queryControls); 
      this.queryAllNodes(queryControls);
    }, {
                  //str = ("\n NOT RUNNING.\n");
      ("\n  NOT RUNNING.\n").post;
    });

    //
    ^str;
  }

  //
  *nodes  { |queryControls|  ^this.default.nodes(queryControls); }  //CLASS



  //------------------------ -o-
  nodesgui   { | interval
               |  
    var  nodesguiWindowName  = "Node Tree$",
         nodesguiRect,
         nodesguiWindow;


    //DEFAULTS.
    interval = interval ?? 0.5;

    if (Parse.isInstanceOfClass(thisFunction, 
          interval, "interval", Number).not,    { ^nil; });

    if (interval < 0, {
      Log.error(thisFunction, 
            "interval must be GREATER THAN ZERO.  (%)", interval
          ).postln; 
                                                  ^nil;
    });


    //
    nodesguiRect = 
      Rect((Window.screenWidth / 4), Window.offsetFromDisplayTop(), 500, 600); 

    nodesguiWindow = 
      Window.findWindowByName(nodesguiWindowName, nodesguiRect, true);


    //
    if (nodesguiWindow.isNil, 
    {
      this.plotTree(interval); 
      nodesguiWindow = 
        Window.findWindowByName(nodesguiWindowName, nodesguiRect, true);
    });


    AppClock.sched(0.0, { |appClockTime| 
      Window.findWindowByName(nodesguiWindowName, nodesguiRect, true);
    });


    //
    ^nodesguiWindow;
    //^nodesguiWindow.front;
  }

  //CLASS
  *nodesgui  { | interval, server
               |  
    server = server ?? Server.default;

    if (Parse.isInstanceOfClass(thisFunction, 
                  server, "server", Server).not,  { ^nil; });

    //
    ^server.nodesgui(interval); 
  }  



  //------------------------ -o-
  channelLevels   { ^this.meter; }
  *channelLevels  { ^this.default.meter; }



  //------------------------ -o-
  channelScope  { |numChannels, index, bufsize, zoom, rate|
    ^this.scope( numChannels  ?? this.options.numOutputBusChannels,
                 index        ?? 0, 
                 bufsize      ?? (4 * 1024), 
                 zoom         ?? 1, 
                 rate         ?? \audio
               );                                              // XXX
  }

  channelScopeControl  { |numChannels, index, bufsize, zoom, rate|
    ^this.channelScope(numChannels, index, bufsize, zoom, rate ?? \control);
  }


  //CLASS
  *channelScope  { |numChannels, index, bufsize, zoom, rate|
    ^this.default.channelScope(numChannels, index, bufsize, zoom, rate);
  }

  *channelScopeControl  { |numChannels, index, bufsize, zoom, rate|
    ^this.default
            .channelScope(numChannels, index, bufsize, zoom, rate ?? \control);
  }



  //------------------------ -o-
  frequencyScope   { 
    var  windowName  = "Freq Analyzer",
         windowRect  = Rect(0, Window.offsetFromDisplayTop, -1, -1),
         window      = Window.findWindowByName(windowName, windowRect, true);


    //
    if (window.isNil, 
    {
      this.freqscope;
      window = Window.findWindowByName(windowName, windowRect, true);
    });


    AppClock.sched(0.0, { |appClockTime| 
      Window.findWindowByName(windowName, windowRect, true);
    });


    ^window;
    //^window.front;
  }

  *frequencyScope  { | server
                     |  
    server = server ?? Server.default;

    if (Parse.isInstanceOfClass(thisFunction, 
                  server, "server", Server).not,  { ^nil; });

    //
    ^server.frequencyScope; 
  }



  //------------------------ -o-
  windowgui   { ^this.makeWindow; }
  *windowgui  { ^this.default.windowgui; }



  //------------------------ -o-
  muteToggle { 
    var  muteState;

    if (this.volume.isMuted, {
      this.unmute;
      muteState = "OFF";

    }, {
      this.mute;
      muteState = "ON";
    });

    ^Log.info(thisFunction, "%: Mute is %", this.name, muteState);
  }

  *muteToggle  { ^this.default.muteToggle; }  //CLASS 



  //------------------------ -o-
  volumegui   { ^this.volume.gui; }
  *volumegui  { ^this.default.volumegui; }


  //------------------------ -o-
  dumpToggle   { | dumpValue
                 |
    if (dumpValue.notNil, { 
      dumpMode = dumpValue;

    }, {
      dumpMode = 
        case
          { dumpMode.isNil; } { 1; }
          { 0 == dumpMode; }  { 1; }
          { 1 == dumpMode; }  { 3; }
          { 3 == dumpMode; }  { 0; }
          { 0; }
        ; //endcase
    });

    //
    ^this.dumpOSC(dumpMode); 
  }

  //
  *dumpToggle  { |dumpValue|  ^this.default.dumpToggle(dumpValue); }




  //---------------------------------------------------------- -o--
  // MOS protocol methods.

  //------------------------ -o-
  pretty  { | pattern, elisionLength, depth,
              casei, compileString, indent, initialCallee, 
              enumerate, bulletIndex, enumerationOffset,
              minimumSpacerSize, nolog, shortVersion
            |
    var  title  = ("% %").format(this.classname.toUpper, this.name);

    var  maxKeyLength  = "aliveThreadIsRunning".size;

    var  uptimeString    = "",
         outputString    = "";

    var  optionKeys      = ServerOptions.instVarNames.asList.sort,
         optionKeyValue  = "",
         optionsArray    = Array.new;

    var  enviroArray,
         enviroA, enviroB, enviroC, enviroD, enviroE, enviroF,
           enviroG, enviroH, enviroI
         ;

    var  str      = "",
         nomatch  = "\n\t(no match)";


    //
    enviroA = (
      allServers:               this.alls,
      allServersRunning:        this.allsRunning,
      defaultServer:            this.class.default.name,
    );
                        // SPACER
    enviroB = (
      serverRunning:            this.serverRunning,
    );

    enviroC = (
      serverBooting:            this.serverBooting,
    );
                        // SPACER

    //
    enviroArray = [ enviroA, (), enviroB, enviroC, () ];

    str = this.prettyLocal( 
            enviroArray, title,
            pattern, elisionLength, depth,
            casei, compileString, indent, initialCallee,
            enumerate, bulletIndex, enumerationOffset, 
            minimumSpacerSize,  true, shortVersion
          );

    //
    if(this.serverRunning, { 
      uptimeString = ("\n\t%\n").format(this.uptime(true));

      if (uptimeString.regexpMatch(pattern, casei).not, {
        uptimeString = "";
      });
    });

    outputString = ("\n\t%\n").format(this.output(true));

    if (outputString.regexpMatch(pattern, casei).not, {
      outputString = "";
    });

    str = ("%%%").format( str, uptimeString, outputString );

                        // SPACER

    //
    enviroD = (
      addr:                    this.addr.pretty,
      isLocal:                 this.isLocal,
      latency:                 this.latency,
      remoteControlled:        this.remoteControlled,
      notify:                  this.notify,
    );


    enviroE = (
      defaultGroup:             this.defaultGroup.asString,
      hasShmInterface:          this.hasShmInterface,
    );

    enviroF = (
      aliveThreadIsRunning:     this.aliveThreadIsRunning,
      aliveThreadPeriod:        this.aliveThreadPeriod,
    );
                        // SPACER
    
    enviroG = (
      sampleRate:              this.sampleRate ?? "(nil)",
    );

    enviroH = (
      actualSampleRate:        this.actualSampleRate ?? "(nil)",
    );

    enviroI = (
      recChannels:              this.recChannels,
      recHeaderFormat:          this.recHeaderFormat,
      recSampleFormat:          this.recSampleFormat,
    );
                        // SPACER


    //
    enviroArray = [     enviroD, enviroE, enviroF,      (), 
                        enviroG, enviroH, enviroI,      (),
                    ];

    str = str ++ this.prettyLocal( 
                   enviroArray, "",
                   pattern, elisionLength, depth,
                   casei, compileString, indent, initialCallee,
                   enumerate, bulletIndex, enumerationOffset, 
                   minimumSpacerSize:maxKeyLength, 
                     nolog:true, shortVersion:shortVersion
                 );


    //
    optionKeys.do({ |key|
      optionKeyValue = ("%.%.options.%")
                           .format(this.classname, this.name, key).compile.();

      if ( key.asString.regexpMatch(pattern, casei)
             || optionKeyValue.asString.regexpMatch(pattern, casei), {
        optionsArray = optionsArray.addAll([key, optionKeyValue]);
      });
    });
        
    if (optionsArray.size > 0, {
      str = ("%\n\tOPTIONS...%%")
                 .format(
                    str, 
                    Dump.argsPaired(optionsArray, indent:"\t   ")
                  );
    });


    //
    if (str.size <= 0, { str = nomatch; });
    ^Log.msg(str);
  }


  //CLASS
  *pretty  { | pattern, elisionLength, depth,
               casei, compileString, indent, initialCallee, 
               enumerate, bulletIndex, enumerationOffset,
               minimumSpacerSize, nolog, shortVersion
             |
    ^this.default.pretty( 
                 pattern, elisionLength, depth,
                 casei, compileString, indent, initialCallee,
                 enumerate, bulletIndex, enumerationOffset,
                 nolog, shortVersion, shortVersion
               );
  }


} //Server

