//
// OSC.sc
//
// Singleton for all things OSC:
//   . System global oscPaths
//   . Monitor all incoming OSC messages (snoopToggle) with combination
//       of allowlists and denylists
//   . Quickly add to, remove from or copy between allowlist and denylist
//   . TraceToggle to flip OSCFunc.trace
//   . Dump OSCFunc defaults (listOfResponders)
//
// Use OSCFunc to add new responders (whether matched or unmatched).
//
//
// NB  Store allowlist/denylist elements as Strings, but operate
//     upon them as Symbols.
//
//
// INTUITION ABOUT DENYLIST/ALLOWLIST--
//   When denylist is enabled... reject all named oscPaths in the 
//     denylist; anything unnamed gets through.  
//   Disabling the denylist accepts everything.
// 
//   When allowlist is enabled... only allow named oscPaths in the
//     allowlist, even if they are also in the denylist.  
//   Disabling the allowlist allows everything that passes the denylist.
// 
//   If denylist is disabled and allowlist is enabled, but empty, nothing
//   comes through.  If denylist is enabled and allowlist is disabled,
//   only oscPaths missing from the denylist are accepted.  If both are
//   disabled, everything comes through; if both are enabled, allowlist is
//   the final arbiter.
//
//
//
// PUBLIC VARIABLES--
//   *synthPath
//
//
// PUBLIC METHODS--
//   *listOfResponders  [*lor]
//
//   *snoop  *snoopToggle
//   *traceToggle
//
//   *allowlistToggle           *denylistToggle
//   *allowlistAdd              *denylistAdd
//   *allowlistAddFromDenyAt    *denylistAddFromAllowAt
//   *allowlistRemove           *denylistRemove
//   *allowlistRemoveAt         *denylistRemoveAt
//   *allowlistClear            *denylistClear
//
//                              *denylistDefaultsAdd
//                              *denylistDefaultsSet
//                              *denylistDefaultsPersistentAdd
//                              *denylistDefaultsOccasionalAdd
//
//   *messageCountMuted     -- Actual messages.
//   *messageCountVerbose   -- Messages about messages: begin/end, count.
//   *messageCountRunningAtBoot
//
//   //NB *Muted and *Verbose are mutually independent.
//
//
//
// HELPER METHODS--
//   *incoming  
//   *listAdd  *toThisListAddFromThatAt 
//   *listRemove  *listRemoveAt
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//   *pretty
//
//
// DEPENDENCIES--
//   Log
//   Object
//   Synth_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


OSC : MobileSound
{
  // Private class variables.
  //
  classvar  classVersion = "0.1";   //RELEASE

  classvar     mainOSCReceiveFunction,
               mainOSCReceivePreviousTime;

  classvar     traceRunning,
               verbose;

               
  // Public class variables.
  //
  classvar  <  oscPathRoot,
            <  oscPathSynth;
                        //ADD

  classvar  <  oscTokenBufferPlaybackIndex;
                        //ADD

  classvar  <> allowlist,
            <> allowlistEnabled,

            <> denylist,
            <> denylistEnabled,
            <  denylistDefaultsPersistent,
            <  denylistDefaultsOccasional;

  classvar  <  messageCountTotal,
            <> messageCountNoticeInterval,
            <  messageCountRunning,
            <> messageCountVerbose,
            <  messageCountMuted,

            <  messageCountRunningAtBoot = true
            //<  messageCountRunningAtBoot = false
               ;




  //--------------------------------------------------------------- -o--
  // Lifecycle, identity, reflection.

  //------------------------ -o-
  *mosVersion { ^super.mosVersion(classVersion); }




  //---------------------------------------------------------- -o--
  // Lifecycle, constructors, class memory.

  //------------------------ -o-
  *initClass  
  {
    //DEFAULTS.
    messageCountRunningAtBoot = messageCountRunningAtBoot ?? false;


    //
    oscPathRoot   = "/mos";
    oscPathSynth  = oscPathRoot +/+ "synth";

    oscTokenBufferPlaybackIndex = "bufferPlaybackIndex";


    //
    allowlist  = Array.new;
    denylist   = Array.new;

    denylistDefaultsPersistent = [
                  "/g_queryTree.reply",   // Node Tree
                  "/localhostInLevels",   // levels (dBFS)
                  "/localhostOutLevels",  // levels (dBFS)
                  "/status.reply",
                        //ADD
                ].sort;

    denylistDefaultsOccasional = [
                  "/b_info",              // Buffer
                  "/d_removed",
                  "/done",
                  "/fail",
                  "/n_end",
                  "/n_go",
                  "/n_off",
                  "/n_on",
                  "/synced",
                        //ADD
                ].sort;

    OSC.denylistDefaultsSet();

    allowlistEnabled  = false;
    denylistEnabled  = true;

    verbose = true;


    //
    mainOSCReceivePreviousTime = 0;


    //
    messageCountRunning         = false;
    messageCountMuted           = false;
    messageCountVerbose         = true;

    messageCountTotal           = -1;      //XXX
    messageCountNoticeInterval  = 10000;   //XXX


    //
    traceRunning = false;


    //
    this.incoming();

  } // *initClass




  //--------------------------------------------------------------- -o--
  // Public class methods.

  //------------------------ -o-
  *listOfResponders  {
    ^AbstractResponderFunc
       .allFuncProxies.pr(depth:2, compileString:false);
  }

  *lor  { ^this.listOfResponders; }   //ALIAS



  //------------------------ -o-
  *snoop  { | value
            |
    //DEFAULTS.
    value = value ?? true;


    //
    if (Parse.isInstanceOfClass(thisFunction, 
                  value, "value", Boolean).not, { ^nil; });

    if ((value xor: messageCountRunning).not, { ^nil; });


    //
    thisProcess.removeOSCRecvFunc(mainOSCReceiveFunction);

    if (value, {
      if (messageCountVerbose, {
        Log.info(thisFunction, 
              "BEGIN snoop on incoming OSC messages...").postln; 
      });

      messageCountTotal    = 0;
      messageCountRunning  = true;

      thisProcess.addOSCRecvFunc(mainOSCReceiveFunction);

    }, {
      messageCountRunning = false;

      if (messageCountVerbose, {
        Log.info(thisFunction, 
              "...END snoop on incoming OSC messages.").postln; 
      });
    });


    //
    ^'';
  }


  //
  *snoopToggle  { | value
                  |
    value = value ?? messageCountRunning.not;

    if (Parse.isInstanceOfClass(thisFunction, 
                  value, "value", Boolean).not, { ^nil; });

    //
    ^this.snoop(value);
  }


  //------------------------ -o-
  *messageCountMuted_  { | value
                         |
     if (Parse.isInstanceOfClass(thisFunction, 
                 value, "value", Boolean ).not,  { ^nil; });

     messageCountMuted = value ?? messageCountMuted.not;

     ^this;
  }


  //------------------------ -o-
  *allowlistToggle  { | value
                      |
     if (Parse.isInstanceOfClass(thisFunction, 
           value, "value", Boolean, isOkayToBeNil:true).not, { ^nil; });

     allowlistEnabled = value ?? allowlistEnabled.not;

     ^this;
  }

  *denylistToggle  { | value
                      |
     if (Parse.isInstanceOfClass(thisFunction, 
           value, "value", Boolean, isOkayToBeNil:true).not, { ^nil; });

     denylistEnabled = value ?? denylistEnabled.not;

     ^this;
  }


  //------------------------ -o-
  *traceToggle  { | value
                  |
    value = value ?? traceRunning.not;

    if (Parse.isInstanceOfClass(thisFunction, 
                  value, "value", Boolean).not, { ^nil; });

    //
    traceRunning = value;
    ^OSCFunc.trace(value);
  }


  //------------------------ -o-
  *allowlistAdd  { |valueOrArray|  ^this.listAdd(valueOrArray, \allowlist); }

  *denylistAdd  { |valueOrArray|  ^this.listAdd(valueOrArray, \denylist); }


  //------------------------ -o-
  *allowlistAddFromDenyAt  { |indexOrArray|  
    ^this.toThisListAddFromThatAt(indexOrArray, \allowlist); 
  }

  *denylistAddFromAllowAt  { |indexOrArray|  
    ^this.toThisListAddFromThatAt(indexOrArray, \denylist); 
  }


  //------------------------ -o-
  *allowlistRemove  { |valueOrArray|  
    ^this.listRemove(valueOrArray, \allowlist); 
  }

  *denylistRemove  { |valueOrArray|  
    ^this.listRemove(valueOrArray, \denylist); 
  }


  //------------------------ -o-
  *allowlistRemoveAt  { |indexOrArray|
    ^this.listRemoveAt(indexOrArray, \allowlist);
  }

  *denylistRemoveAt  { |indexOrArray|
    ^this.listRemoveAt(indexOrArray, \denylist);
  }


  //------------------------ -o-
  *allowlistClear  { allowlist = []; }

  *denylistClear  { denylist = []; }


  //------------------------ -o-
  *denylistDefaultsPersistentAdd  { 
    OSC.denylistAdd(denylistDefaultsPersistent); 
  }

  *denylistDefaultsOccasionalAdd  { 
    OSC.denylistAdd(denylistDefaultsOccasional); 
  }

  //
  *denylistDefaultsAdd  { | addAll
                           |
    //DEFAULTS.
    addAll = addAll ?? false;

    if (Parse.isInstanceOfClass(thisFunction, 
                  addAll, "addAll", Boolean).not, { ^nil; });

    //
    OSC.denylistDefaultsPersistentAdd(); 

    if (addAll, {
      OSC.denylistDefaultsOccasionalAdd(); 
    });
  }

  //
  *denylistDefaultsSet  { | addAll
                           |
    //DEFAULTS.
    addAll = addAll ?? false;

    if (Parse.isInstanceOfClass(thisFunction, 
                  addAll, "addAll", Boolean).not, { ^nil; });

    //
    OSC.denylistClear();
    OSC.denylistDefaultsAdd(addAll);
  }




  //---------------------------------------------------------- -o--
  // Universal patterns.
                                        //ADD

  //------------------------- -o-
  // oscPathSynth + synth.defName [+ oscToken]
  //
  *synthPath  { | synth, 
                  oscToken
                |
    var  path;


    //DEFAULTS.
    oscToken = oscToken ? "";

    //
    if (Parse.isInstanceOfClass(thisFunction, 
                  synth, "synth", Synth).not, { ^''; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  oscToken, "oscToken", String).not, { ^''; });


    //
    path = OSC.oscPathSynth +/+ synth.defName;

    if (oscToken.size > 0, { path = path +/+ oscToken; });

    ^path;
  }




  //---------------------------------------------------------- -o--
  // Helper methods.

  //------------------------ -o-
  // NB  Named to set thisFunction used within mainOSCReceiveFunction.
  //
  *incoming  
  { 
    mainOSCReceiveFunction = { | msg, time, replyAddr, recvPort
                               |
      var  presentMessage,
           timeDifferenceSinceLastMessageReceived  
                           = time - mainOSCReceivePreviousTime;

      mainOSCReceivePreviousTime  = time;


      //
      messageCountTotal = messageCountTotal + 1;
                 
      if (messageCountVerbose, {
        if ((messageCountTotal % messageCountNoticeInterval) == 0, {
          Log.info(thisFunction, 
                "messageCountTotal is %.", messageCountTotal).postln;
        });
      });


      //
      if (denylistEnabled, {
        presentMessage = true;

        denylist.do({ |elem|
          if (msg[0] == elem.asSymbol, { presentMessage = false; });
        });

      }, {
        presentMessage = true;
      });


      if (allowlistEnabled, {
        presentMessage = false;

        allowlist.do({ |elem|
          if (msg[0] == elem.asSymbol, { presentMessage = true; });
        });
      });

    
      //
      if (presentMessage && messageCountMuted.not, {
        var  difference  = timeDifferenceSinceLastMessageReceived
                                                    .roundPadRight(0.001);

        if ("0" == difference, { difference = "0    "; });

        //
        Log.info( thisFunction, 
                  "at % (%) on port % from % :: %", 
                     time.roundPadRight(0.001), difference,
                     recvPort, replyAddr.pr.removeWhitespace,
                     msg
                ).postln;
      });

    };  //mainOSCReceiveFunction


    // 
    ^this; 
  }


  //------------------------ -o-
  *listAdd  { | valueOrArray,           
                allowOrDenyList
              |
    var  theList,
         theListName;


    //
    if (Parse.isInstanceOfArrayOfTypeWithSize(thisFunction, 
          valueOrArray, "valueOrArray", [Symbol, String], minSize:1).not, 
                                                                { ^''; });

    if (Parse.isInstanceOfClass(thisFunction, 
          allowOrDenyList, "allowOrDenyList", Symbol).not,    { ^''; });
                

    //
    valueOrArray = valueOrArray.intoArray;

    case
      { \allowlist == allowOrDenyList; }
          {
            theList      = allowlist;
            theListName  = "allowlist";
          }

      { \denylist == allowOrDenyList; }
          {
            theList      = denylist;
            theListName  = "denylist";
          }

      {
        Log.error( thisFunction, 
                   "allowOrDenyList must be %allowlist or %denylist.  (%)", 
                      $\\, $\\, allowOrDenyList.prettyShort
                 ).postln; 
                                                                  ^'';
      }
    ; //endcase


    //
    valueOrArray.do({ |elem|
      var  elemStr                  = elem.asString,
           convertedToAbsolutePath  = false;

      if (elemStr.isAbsolutePath.not, 
      {
        elemStr = ("/%").format(elemStr);
        convertedToAbsolutePath = true;
      });

      if (theList.select({ |elem|  elemStr == elem; }).size <= 0, 
      {
        theList = theList.add(elemStr).sort;

        if (\allowlist == allowOrDenyList, {
          allowlist = theList;
        }, {
          denylist = theList;
        });

        if (convertedToAbsolutePath && verbose, {
          Log.warning(thisFunction, 
                "Converted \"%\" to absolute path.", elem).postln; 
        });

      }, {
        Log.warning( thisFunction, 
              "Element already exists in %. (%)", theListName, elemStr).postln; 
      });

    }); //valueOrArray.do


    //
    ^this;

  } // *listAdd


  //------------------------ -o-
  *toThisListAddFromThatAt  { | indexOrArray,
                                allowOrDenyList
                              |
    var  thatList,
         thatListName;

    var  validatorFunctionString  = { | logContext, 
                                        value, valueName, 
                                        classRequirement, 
                                        validatorContextArray
                                      |
           var  thatList      = validatorContextArray[0],
                thatListName  = validatorContextArray[1];

           var  rval  = (value < thatList.size) && (value >= 0);

           if (rval.not, {
             Log.error( logContext, 
                        "value index (%) OUT OF RANGE for size of Array % (%).",
                             value, thatListName, thatList.size
                      ).postln; 
           });

           rval;
         }.cs;


    //
    if (Parse.isInstanceOfClass(thisFunction, 
          allowOrDenyList, "allowOrDenyList", Symbol).not, { ^''; });
                
    case
      { \allowlist == allowOrDenyList; }
          { 
            thatList      = denylist;
            thatListName  = "denylist";
          }

      { \denylist == allowOrDenyList; }
          { 
            thatList      = allowlist;
            thatListName  = "allowlist";
          }

      {
        Log.error( thisFunction, 
                   "allowOrDenyList must be %allowlist or %denylist.  (%)", 
                      $\\, $\\, allowOrDenyList.prettyShort
                 ).postln; 
        ^'';
      }
    ; //endcase

    //
    if (Parse.isInstanceOfArrayOfTypeWithSize(thisFunction,
                  indexOrArray, "indexOrArray", Integer, 
                  validatorFunctionString:validatorFunctionString, 
                  validatorContextArray:[thatList, thatListName],
                  minSize:1).not, 
                    { ^''; });

    indexOrArray = indexOrArray.intoArray;


    //
    indexOrArray.do({ |index|
      if (\allowlist == allowOrDenyList, {
        OSC.allowlistAdd(denylist[index]);
      }, {
        OSC.denylistAdd(allowlist[index]);
      });
    });


    //
    ^this;

  }  // *toThisListAddFromThatAt


  //------------------------ -o-
  // NB  No warning if listed elements do not exist.
  //
  *listRemove  { | valueOrArray,
                   allowOrDenyList
                 |
    var  theList;

    //
    if (Parse.isInstanceOfArrayOfTypeWithSize(thisFunction,
          valueOrArray, "valueOrArray", [String, Symbol], minSize:1).not, 
            { ^''; });

    if (Parse.isInstanceOfClass(thisFunction, 
          allowOrDenyList, "allowOrDenyList", Symbol).not, { ^''; });
                

    //
    valueOrArray = valueOrArray.intoArray;

    case
      { \allowlist == allowOrDenyList; }
          { theList = allowlist; }

      { \denylist == allowOrDenyList; }
          { theList = denylist; }

      {
        Log.error( thisFunction, 
                   "allowOrDenyList must be %allowlist or %denylist.  (%)", 
                      $\\, $\\, allowOrDenyList.prettyShort
                 ).postln; 
        ^'';
      }
    ; //endcase


    //
    valueOrArray.do({ |candidate|  
      theList = theList.reject({ |elem|  elem == candidate; })
    });

    if (\allowlist == allowOrDenyList, {
      allowlist = theList;
    }, {
      denylist = theList;
    });

    
    //
    ^this;

  }  // *listRemove


  //------------------------ -o-
  *listRemoveAt  { | indexOrArray,
                     allowOrDenyList
                   |
    var  theList,
         theListName,
         elementsToRemove  = Array.new;

    var  validatorFunctionString  = { | logContext, 
                                        value, valueName, 
                                        classRequirement, 
                                        validatorContextArray
                                      |
           var  theList      = validatorContextArray[0],
                theListName  = validatorContextArray[1];

           var  rval  = (value < theList.size) && (value >= 0);

           if (rval.not, {
             Log.error(logContext, 
               "value index (%) OUT-OF-RANGE for size of Array % (%).",
                  value, theListName, theList.size
             ).postln; 
           });

           rval;
         }.cs;


    //
    if (Parse.isInstanceOfClass(thisFunction, 
          allowOrDenyList, "allowOrDenyList", Symbol).not, { ^''; });
                
    case
      { \allowlist == allowOrDenyList; }
          { 
            theList      = allowlist;
            theListName  = "allowlist";
          }

      { \denylist == allowOrDenyList; }
          { 
            theList      = denylist;
            theListName  = "denylist";
          }

      {
        Log.error( thisFunction, 
                   "allowOrDenyList must be %allowlist or %denylist.  (%)", 
                      $\\, $\\, allowOrDenyList.prettyShort
                 ).postln; 
        ^'';
      }
    ; //endcase

    //
    if (Parse.isInstanceOfArrayOfTypeWithSize(thisFunction,
                  indexOrArray, "indexOrArray", Integer, 
                  validatorFunctionString:validatorFunctionString, 
                  validatorContextArray:[theList, theListName],
                  minSize:1).not, 
                    { ^''; });

    indexOrArray = indexOrArray.intoArray;


    //
    indexOrArray.do({ |index|
      elementsToRemove = elementsToRemove.add(theList[index]);
    });

    elementsToRemove.do({ |elem|  theList.remove(elem); });


    if (\allowlist == allowOrDenyList, {
      allowlist = theList;
    }, {
      denylist = theList;
    });


    //
    ^this;

  }  // *listRemoveAt




  //---------------------------------------------------------- -o--
  // MOS protocol methods.

  //------------------------ -o-
  *pretty  { | pattern, elisionLength, depth,
               casei, compileString, indent, initialCallee, 
               enumerate, bulletIndex, enumerationOffset,
               minimumSpacerSize, nolog, shortVersion
             |
    var  title  = this.classname.toUpper;

    var  maxKeyLength  = "oscTokenBufferPlaybackIndex".size;

    var  allowlistOrString,
         denylistOrString;

    var  enviroA, 
         enviroB,
         enviroC,
         enviroD,
         enviroE1, enviroE2,
         enviroF1, enviroF2, enviroF3, enviroF4,
         enviroArray;


    //
    allowlistOrString = if (allowlist.size <= 0, {
      allowlistOrString = Object.labelEmpty;
    }, {
      allowlistOrString = allowlist;
    });

    denylistOrString = if (denylist.size <= 0, {
      denylistOrString = Object.labelEmpty;
    }, {
      denylistOrString = denylist;
    });


    //
    enviroA = (
      oscPathRoot:   oscPathRoot,
      oscPathSynth:  oscPathSynth,
    );

                //SPACER

    enviroB = (
      oscTokenBufferPlaybackIndex:  oscTokenBufferPlaybackIndex,
    );

                //SPACER

    enviroC = (
      allowlist:         allowlistOrString,
    );

                //SPACER

    enviroD = (
      denylist:         denylistOrString,
    );

                //SPACER

    enviroE1 = (
      allowlistEnabled:  allowlistEnabled,
    );

    enviroE2 = (
      denylistEnabled:  denylistEnabled,
    );

                //SPACER

    enviroF1 = (
      messageCountRunning:         messageCountRunning,
      messageCountRunningAtBoot:   messageCountRunningAtBoot,
    );

    enviroF2 = (
      messageCountMuted:           messageCountMuted,
      messageCountVerbose:         messageCountVerbose,
    );

    enviroF3 = (
      messageCountTotal:           messageCountTotal,
    );

    enviroF4 = (
      messageCountNoticeInterval:  messageCountNoticeInterval,
    );


    enviroArray = [ enviroA, (),
                    enviroB, (),
                    enviroC, (),
                    enviroD, (),
                    enviroE1, enviroE2, (),
                    enviroF1, enviroF2, enviroF3, enviroF4,
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


} // OSC

