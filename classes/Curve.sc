//
// Curve.sc
//
// Curve manages a control bus and engages a Synth to change its value.
//
// By DEFAULT a Curve instance runs for one second, creating a 
//   linear curve in the range [0,1].
//
//
//
// MOS CALLER PROTOCOL--
//   configurationComplete() returns the control bus, it MAY BE RUN 
//   when Curve is assigned.  This method allows the caller to optionally:
//     1) Adapt Curve range to its own range, and/or;
//     2) Start the curve immediately.
//
//
//
// PUBLIC CLASS METHODS--
//   *new  
//   *usage
//   *demo
//
// PRIVATE CLASS METHODS--
//   *createSynthDefWithWrap  
//   *createSynthDefs
//
//
// PUBLIC INSTANCE METHODS--
//   configurationComplete
//   run  stop  runStop
//   value  
//   release  
//   isPlaying  isRunning  
//   plot  
//
// PRIVATE INSTANCE METHODS--
//   initCurve  
//   instanceArgs  
//   isAlreadyReleased  
//   setBus  
//
//
//
// MOS PROTOCOL SUPPORT--
//   initCurve
//   (*)mosVersion
//   (*)pretty
//   printOn  storeOn  storeArgs
//
//
//
// SYNTH DEFINITIONS--
//   \mosSynthCurveLinearUpwards
//   \mosSynthCurveLinearDownwards
//   \mosSynthCurveExponentialUpwards
//   \mosSynthCurveExponentialDownwards
//
//
//
// CLASS DEPENDENCIES--
//   Group_MOS
//   Log 
//   MobileSound
//   Node_MOS
//   Object_MOS
//   Parse
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------

Curve : MobileSound
{
  classvar  classVersion  = "0.2";   //RELEASE

  classvar  invocationArgs;



  // Class support variables.
  //
  classvar  <  curveDirectionEnum,
            <  curveTypeEnum;

  classvar  <> group;



  // Initialization variables.
  //
  var  < durationInSeconds,
       < curveDirection,
       < curveType,

       < range,
       < mul,
       < add,

       < activateUponAssignment,        
       < normalizeToRemainingRange,    
       < server;


  // Internal variables.
  //
  var  < bus;
  var  < synth;

  var  < isReleased;




  //---------------------------------------------------------- -o--
  *mosVersion { ^super.mosVersion(classVersion); }
  mosVersion  { ^this.class.mosVersion; }




  //---------------------------------------------------------- -o--
  // Constructors, state.

  //------------------------ -o-
  *initClass  
  {
    invocationArgs = 
      "[durationInSeconds, curveDirection, curveType, range, mul, add, activateUponAssignment, normalizeToRemainingRange, server]";

    curveDirectionEnum  = [ \fadeIn, \fadeOut ];
    curveTypeEnum       = [ \linear, \exponential ];

    group = nil;

    //
    StartUp.add  {
      this.createSynthDefs();
    }
  }


  //------------------------ -o-
  *new  { | durationInSeconds, 
            curveDirection,
            curveType,
            range,
            mul,
            add,
            activateUponAssignment,
            normalizeToRemainingRange,
            server
          |
    ^super.new.initCurve( durationInSeconds, 
                          curveDirection,
                          curveType,
                          range,
                          mul,
                          add,
                          activateUponAssignment,
                          normalizeToRemainingRange,
                          server
                        );
  }


  //------------------------ -o-
  initCurve  { | durationInSeconds,
                 curveDirection,
                 curveType,
                 range,
                 mul,
                 add,
                 activateUponAssignment,
                 normalizeToRemainingRange,
                 server
               |

    //DEFAULTS.
    isReleased = false;

    this.durationInSeconds  = durationInSeconds  ?? 1.0;
    this.curveDirection     = curveDirection     ?? \fadeIn;
    this.curveType          = curveType          ?? \linear;

    this.range              = range              ?? [0.0, 1.0];
    this.mul                = mul                ?? 1.0;
    this.add                = add                ?? 0.0;

    this.activateUponAssignment     = activateUponAssignment     ?? false;
    this.normalizeToRemainingRange  = normalizeToRemainingRange  ?? false;

    this.server = server ?? Server.default;

    group  = this.class.createGroup(server:server, verbose:false);
    bus    = Bus.newControlOnServer(server);

    this.setBus();

    //
    if (    this.durationInSeconds.isNil
         || this.curveDirection.isNil
         || this.curveType.isNil
         || this.range.isNil
         || this.mul.isNil
         || this.add.isNil
         || this.activateUponAssignment
         || this.normalizeToRemainingRange
         || this.server.isNil
         || group.isNil
         || bus.isNil
          ,
    {
      Log.error(thisFunction, "Initialization FAILED.").postln;
      ^nil;
    });

    ^this;
  }


  //------------------------ -o-
  release  {
    if (this.isAlreadyReleased, { ^nil; });

    //
    bus.free;
    bus = nil;

    if (synth.notNil, { 
      if (synth.isPlaying, { synth.free; });
      synth = nil;
    });

    isReleased = true;

    ^nil;
  }


  //------------------------ -o-
  instanceArgs  { | asStringOrArray
                  |
    switch (asStringOrArray, 
      //
      \string, 
         { ^("%, %, %, %, %, %, %, %, Server.%")
               .format( 
                  durationInSeconds.pretty,
                  curveDirection.pretty,
                  curveType.pretty,
                  range.pretty(depth:0),
                  mul.pretty,
                  add.pretty,
                  activateUponAssignment,
                  normalizeToRemainingRange,
                  server
               );
         },

      //
      \array,  
         {
           ^[ durationInSeconds,
              curveDirection,
              curveType,
              range,
              mul,
              add,
              activateUponAssignment,
              normalizeToRemainingRange,
              server
            ];
         },
    );


    //
    ^Log.error(thisFunction, "asStringOrArray MUST be \string or \array");

  } //instanceArgs




  //---------------------------------------------------------- -o--
  // Class support methods.
  //
  // printOn generated by .asString.
  // storeOn generated by .asCompileString.
  //

  //------------------------ -o-
  *usage  { ^("USAGE: %.new(%)").format(this.classname, invocationArgs); }

  //
  printOn  { |stream|  stream << ("%").format(this.classname); }

  //
  storeOn  { |stream|                           
    stream << ("%.new(%)").format(this.classname, this.instanceArgs(\string));
  }

  //
  storeArgs  { |stream|  ^this.instanceArgs(\array); }




  //---------------------------------------------------------- -o--
  // Getters/setters.

  //------------------------ -o-
  durationInSeconds_  { | value
                        |
    //
    if (this.isAlreadyReleased, { ^nil; });

    this.isRunning(true);


    //
    case
      //ERRORS.
      { Parse.isInstanceOfClass(
              thisFunction, value, "durationInSeconds", Number ).not; 
      }
          { /*EMPTY*/ }

      { value <= 0; }
          { Log.error(thisFunction, 
                  "durationInSeconds MUST be greater than zero.  (%)", value
                     ).postln;
          }

      //SUCCESS.
      { durationInSeconds = value; }

    ; //endcase


    ^this;
  }


  //------------------------ -o-
  curveDirection_  { | value
                    |
    //
    if (this.isAlreadyReleased, { ^nil; });

    this.isRunning(true);


    //
    case
      //ERRORS.
      { Parse.isInstanceOfClass(
              thisFunction, value, "curveDirection", Symbol ).not;
      }
          { /*EMPTY*/ }

      { this.class.curveDirectionEnum.includes(value).not; }
          { Log.error(thisFunction, 
                        "curveDirection MUST be in the set: %.  (%)",
                           this.class.curveDirectionEnum.pr(0), value
                     ).postln;
          }

      //SUCCESS.
      { curveDirection = value; 
        this.setBus();
      }

    ; //endcase


    ^this;
  }


  //------------------------ -o-
  curveType_  { | value
                |
    //
    if (this.isAlreadyReleased, { ^nil; });

    this.isRunning(true);


    //
    case
      //ERROR.
      { Parse.isInstanceOfClass(
              thisFunction, value, "curveType", Symbol ).not;
      }
          { /*EMPTY*/ }

      { this.class.curveTypeEnum.includes(value).not; }
          { Log.error(thisFunction, 
                        "curveType MUST be in the set: %.  (%)",
                           this.class.curveTypeEnum.pr(0), value
                     ).postln;
          }

      //SUCCESS.
      { curveType = value; 
        this.setBus();
      }

    ; //endcase


    ^this;
  }


  //------------------------ -o-
  //NB  range is always defined as [low, high], regardless of curveDirection.
  //
  range_  { | arrayPairValue
            |
    //
    if (this.isAlreadyReleased, { ^nil; });

    this.isRunning(true);


    //
    case
      { Parse.isInstanceOfArrayOfTypeWithSize(thisFunction, 
                   arrayPairValue, "range", Number, size:2).not; 
      }
          { /*EMPTY*/ }

      { arrayPairValue[0] > arrayPairValue[1]; }
          { Log.error(thisFunction, 
                     "range must be a pair of monotonically "
                  ++ "increasing Numbers.  (%)",
                         arrayPairValue
                ).postln; 
          }

      { range = arrayPairValue; 
        this.setBus();
      }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  mul_  { | value
          |
    //
    if (this.isAlreadyReleased, { ^nil; });

    this.isRunning(true);


    //
    case
      { Parse.isInstanceOfClass(thisFunction, value, "mul", Number ).not; }
          { /*EMPTY*/ }

      { mul = value; 
        this.setBus();
      }
    ; //endcase


    ^this;
  }


  //------------------------ -o-
  add_  { | value
          |
    //
    if (this.isAlreadyReleased, { ^nil; });

    this.isRunning(true);


    //
    case
      { Parse.isInstanceOfClass(thisFunction, value, "add", Number ).not; }
          { /*EMPTY*/ }

      { add = value; 
        this.setBus(); 
      }
    ; //endcase


    ^this;
  }


  //------------------------ -o-
  activateUponAssignment_  { | value
                             |
    if (this.isAlreadyReleased, { ^nil; });

    //
    case
      //ERRORS.
      { Parse.isInstanceOfClass(
          thisFunction, value, "activateUponAssignment", Boolean ).not; 
      }
          { /*EMPTY*/ }

      //SUCCESS.
      { activateUponAssignment = value; }

    ; //endcase

    //
    ^this;
  }


  //------------------------ -o-
  normalizeToRemainingRange_  { | value
                                |
    if (this.isAlreadyReleased, { ^nil; });

    //
    case
      //ERRORS.
      { Parse.isInstanceOfClass(
          thisFunction, value, "normalizeToRemainingRange", Boolean ).not; 
      }
          { /*EMPTY*/ }

      //SUCCESS.
      { normalizeToRemainingRange = value; }

    ; //endcase

    //
    ^this;
  }


  //------------------------ -o-
  server_  { | value
             |
    //
    if (this.isAlreadyReleased, { ^nil; });

    this.isRunning(true);


    //
    case
      { Parse.isInstanceOfClass(thisFunction, value, "server", Server ).not; }
          { /*EMPTY*/ }

      { server = value; }
    ; //endcase


    ^this;
  }




  //---------------------------------------------------------- -o--
  // Private class methods.

  //------------------------ -o-
  *createSynthDefs  
  { 
    var  synthName,
         wrappedSynth;


    //
    synthName = \mosSynthCurveLinearUpwards;

    wrappedSynth = 
      { | durationInSeconds, rangeOffset, rangeLow, rangeHigh, mul, add
        |
        var  line  = Line.kr( rangeLow, rangeHigh, 
                              durationInSeconds, 
                              mul, add, 
                              doneAction:2 
                            );
        line;
      };

    this.createSynthDefWithWrap(synthName, wrappedSynth);


    //
    synthName = \mosSynthCurveLinearDownwards;

    wrappedSynth = 
      { | durationInSeconds, rangeOffset, rangeLow, rangeHigh, mul, add
        |
        var  line  = Line.kr( rangeHigh, rangeLow, 
                              durationInSeconds, 
                              mul, add, 
                              doneAction:2 
                            );
        line;
      };

    this.createSynthDefWithWrap(synthName, wrappedSynth);


    //NB  rangeOffset keeps XLine from generating signal "nan".
    //
    synthName = \mosSynthCurveExponentialUpwards;

    wrappedSynth = 
      { | durationInSeconds, rangeOffset, rangeLow, rangeHigh, mul, add
        |
        var  line  = XLine.kr(  rangeLow+rangeOffset, rangeHigh+rangeOffset, 
                                durationInSeconds, 
                                mul, add, 
                                doneAction:2 
                             );
        line;
      };

    this.createSynthDefWithWrap(synthName, wrappedSynth);


    //
    synthName = \mosSynthCurveExponentialDownwards;

    wrappedSynth = 
      { | durationInSeconds, rangeOffset, rangeLow, rangeHigh, mul, add
        |
        var  line  = XLine.kr(  rangeHigh+rangeOffset, rangeLow+rangeOffset, 
                                durationInSeconds, 
                                mul, add,
                                doneAction:2 
                             );
        line;
      };

    this.createSynthDefWithWrap(synthName, wrappedSynth);


    //
    ^'';
  }


  //------------------------ -o-
  *createSynthDefWithWrap  { | synthName,
                               wrappedSynthDefFunction
                             |
    //
    SynthDef(synthName, { | controlBus=9999, 
                            durationInSeconds,
                            rangeLow,
                            rangeHigh,
                            mul,
                            add
                          |

      var  rangeOffset  = 1 / SampleRate.ir;

      var  rangeLowExact   = (rangeLow * mul) + add,
           rangeHighExact  = (rangeHigh * mul) + add;

      var  out;

      //
      out = SynthDef.wrap( wrappedSynthDefFunction,
                           prependArgs: [ durationInSeconds, 
                                          rangeOffset, 
                                          rangeLow, rangeHigh, 
                                          mul, add 
                                        ]
                         );

      out = Clip.kr(out, rangeLowExact, rangeHighExact);

      ReplaceOut.kr(controlBus, out);
    }).store;

    ^'';
  }




  //---------------------------------------------------------- -o--
  // Public instance methods.

  //------------------------ -o-
  // RETURN:  this.bus  On success;
  //          nil       Otherwise.
  //
  // ASSUMES  Receiver always calls this method when Curve is assigned.
  //
  // normalizeToRemainingRange means the Bus should not assert its 
  //   absolute values, but apply its curve to the distance between 
  //   the current value of the object to which it is assigned and 
  //   the maximum of that same object.
  // activateUponAssignment means the Bus should start running the 
  //   moment it is assigned.
  //
  // XXX  If chosen, bus.run() begins here before it is assigned in
  //      the calling environment.
  //
  configurationComplete  { | curveTargetCurrentValue,
                             curveTargetRange
                           |
    //DEFAULTS.
    curveTargetRange = curveTargetRange ?? [0.0, 1.0];

    if (Parse.isInstanceOfClass(thisFunction, 
                  curveTargetCurrentValue, "curveTargetCurrentValue", 
                  Number).not,                                  { ^nil }); 

    if (Parse.isInstanceOfArrayOfTypeWithSize(thisFunction, 
                  curveTargetRange, "curveTargetRange", Number, 
                  size:2).not,                                  { ^nil; });

    if (curveTargetRange[0] >= curveTargetRange[1], 
    {
      Log.error(thisFunction, 
               "curveTargetRange MUST represent a "
            ++ "minimum/maximum with unique values.  (%)", curveTargetRange
          ).postln; 
                                                                  ^nil; 
    });


    //
    if (this.normalizeToRemainingRange, 
    {
      Log.warning(thisFunction, 
            "Resetting parameters per calling environment."
          ).postln; 

      this.add  = 0.0;
      this.mul  = 1.0;

      switch(this.curveDirection, 
        \fadeIn, {
            this.range = [curveTargetCurrentValue, curveTargetRange[1]];
          },

        \fadeOut, {
            this.range = [curveTargetRange[0], curveTargetCurrentValue];
          }
      );

    }); 


    //
    if (this.activateUponAssignment, {
      this.run();
    }, {
      this.bus.set(curveTargetCurrentValue);
    });


    //
    ^this.bus;
  }


  //------------------------ -o-
  run  {                                                //FORKS
    var  synthType;
    var  synthTypePrefix     = "mosSynthCurve";

    var  synthTypeCurve      = "Linear";
    var  synthTypeDirection  = "Upwards";


    //
    if (this.isAlreadyReleased, { ^nil; });

    if (this.class.isGroupPlaying.not, {
      Log.warning( thisFunction, 
                   "Curve.group is NOT PLAYING.  Recreating group..."
                 ).postln; 

      this.class.createGroup(server:this.server);
    });

    if (synth.isPlaying, { 
      if (synth.isRunning, {
        Log.warning(thisFunction, "synth EXISTS and is ACTIVE.").postln; 
      }, {
        synth.pauseContinue();
      });

      ^this; 

    }, {
      if (synth.notNil, {
        synth.free;
        synth = nil;
      });
    });


    //
    if (\linear != curveType,     { synthTypeCurve      = "Exponential"; });
    if (\fadeIn != curveDirection, { synthTypeDirection  = "Downwards"; });

    synthType = 
           ("%%%").format(synthTypePrefix, synthTypeCurve, synthTypeDirection)
                  .asSymbol;

    //
    {
      Routine.spinlock({ this.class.group.notNil; });

      synth = Synth.newRegistered(
                synthType,

                [ \controlBus,         bus, 
                  \durationInSeconds,  durationInSeconds,
                  \rangeLow,           range[0],
                  \rangeHigh,          range[1],
                  \mul,                mul,
                  \add,                add,
                ],

                group,
                \addToHead,

                server
              );
    }.fork;

    ^this;
  }


  //------------------------ -o-
  stop  {
    if (this.isAlreadyReleased, { ^nil; });

    synth.pauseContinue(false); 
    ^this;
  }


  //------------------------ -o-
  //NB  Will restart Curve if it has completed.
  //
  runStop  { 
    if (this.isAlreadyReleased, { ^nil; });

    if (synth.isPlaying(), {
      synth.pauseContinue(); 
    }, {
      this.run();
    });

    ^this;
  }


  //------------------------ -o-
  value  
  { 
    if (this.isAlreadyReleased, { ^nil; });

    //
    if (server.hasShmInterface.not, { ^Object.labelUndefined; });

    ^bus.getSynchronous().round(1e-12);
  }


  //------------------------ -o-
  isPlaying  { | verbose=true
               |
    if (this.isAlreadyReleased(verbose), { ^false; });
    ^Node.isPlaying(synth);
  }


  //------------------------ -o-
  // INPUT verbose: 
  //   true   Print everything;
  //   false  Print nothing;
  //   nil    Print released warning, supress parameters warning.
  //
  isRunning  { | verbose
               |
    var  synthIsRunning  = Node.isRunning(synth);

    if (this.isAlreadyReleased(verbose ?? true), { ^false; });

    verbose = verbose ?? false;

    if (synthIsRunning && verbose, {
      Log.warning(thisFunction, 
               "Parameter changes will not take effect "
            ++ "until the current run is completed."
          ).postln; 
    });

    ^synthIsRunning;
  }


  //------------------------ -o-
  // An approximate plot of this Curve instance.
  //
  plot  {                                               //FORKS
    var  exampleCurve  = Curve.new( 1.0,   //XXX
                                    this.curveDirection, 
                                    this.curveType, 
                                    this.range,
                                    this.mul, 
                                    this.add,
                                    this.activateUponAssignment,
                                    this.normalizeToRemainingRange,
                                    this.server
                                  );

    var  valueArray   = Array.new,
         lastIndex    = nil;

    var  synthName    = nil;

    var  plotRoutine  = Routine({ |appClockTime|  
                          var  plotName  = 
                                     ("%  (% samples over 1 second)")
                                        .format(synthName, valueArray.size);

                          var  v  = valueArray.plot(name:plotName);

                          v.domainSpecs = 
                              ControlSpec(0, this.durationInSeconds);
                          v.refresh;

                          exampleCurve.release;
                        });

    var  initialWait   = 0.010,
         intervalWait  = 0.010;


    //
    {
      exampleCurve.run;
      initialWait.wait;

      Routine.spinlock(
                { exampleCurve.isRunning; },
                testIterationWait:intervalWait
              );

      synthName = exampleCurve.synth.defName;


      //
      block  { |break|
        loop {
          if (exampleCurve.isRunning.not, { break.(); });

          valueArray = valueArray.add(exampleCurve.value);
          intervalWait.wait;
        };
      };


      // Remove leading and trailing duplicates.
      //
      while ( { valueArray[0] == valueArray[1]; }, {
        valueArray.removeAt(1);
      });

      lastIndex = valueArray.size - 1;

      while ( { valueArray[lastIndex] == valueArray[lastIndex - 1]; }, {
        valueArray.removeAt(lastIndex - 1);
        lastIndex = valueArray.size - 1;
      });


      //
      AppClock.play(plotRoutine);
    }.fork;


    //
    ^this;
  }




  //---------------------------------------------------------- -o--
  // Private instance methods.

  //------------------------ -o-
  isAlreadyReleased  { | verbose=true
                       |
    if (isReleased, {
      if (verbose, {
        Log.error(thisFunction, 
                    "% is NO LONGER ACTIVE.", this.classname).postln;
      });

      ^true;
    });

    ^false;
  }


  //------------------------ -o-
  // ASSUME all elements of bus.set() equasion will eventually be defined.  
  // Exit if any are undefined.
  //
  setBus  {
    var  rangeStart;

    if (    curveDirection.isNil 
         || range.isNil 
         || mul.isNil 
         || add.isNil   
         || server.isNil
         ,                      { ^this; } );

    //
    if (\fadeIn == this.curveDirection, {
      rangeStart = range[0];
    }, {
      rangeStart = range[1];
    });

    bus.set( (rangeStart * mul) + add );

    ^this;
  }



  //---------------------------------------------------------- -o--
  // Demo.

  *demo                                                 //FORKS
  {
    var  functionContext        = thisFunction,
         oscMutedPreviousValue  = OSC.messageCountMuted;


    //
    OSC.messageCountMuted = true;

    "".postln;
    Log.info(functionContext, "BEGIN.\n").postln; 

    Curve.usage.post;


    //
    {
      var  curveObj, synthObj;

      var  freqBase                = 200,
           waitTimeForConvergence  = 10.0;


      //NB  Exponential curves converge before their target goal 
      //    by (1 / SampleRate.ir).
      //
      curveObj = Curve(10, \fadeOut, \exponential, mul:400, add:freqBase);
      0.25.wait;

      Curve.pretty.postln;
      curveObj.pretty.postln;
      "".postln;

      curveObj.plot;
        
      synthObj = Synth.newRegistered(\default);
      synthObj.map(\freq, curveObj.bus);


      // Continuously post Curve value.
      //
      {
        while ( { curveObj.isReleased(verbose:true).not }, 
        {
          if (curveObj.value > freqBase,
          {
            var  str  = ("curveObj.value = %").format(curveObj.value);
            Log.info(functionContext, str).postln; 
            0.5.wait;
          });
        });
      }.fork;


      //
      curveObj.run;
      
      Routine.spinlock({ synthObj.isRunning });
      1.wait;

      synthObj.pretty.postln;
      curveObj.pretty.postln;
      "".postln;


      // Wait to release resources.
      //
      {
        Routine.spinlock(
                  { curveObj.value <= freqBase; }, 
                  testIterationWaitMax:waitTimeForConvergence,
                  disallowExceptionError:true
                );

        "".postln;
        Log.info(functionContext, "FREEING objects...").postln; 

        synthObj.free;
        synthObj.pretty.postln;

        curveObj.release();


        //
        "".postln;
        Log.info(functionContext, "END.\n").postln; 

        OSC.messageCountMuted = oscMutedPreviousValue;
      }.fork;
    }.fork;


    '';
  }  // *demo




  //---------------------------------------------------------- -o--
  // MOS protocol methods.

  //------------------------ -o-
  pretty  { | pattern, elisionLength, depth,
              casei, compileString, indent, initialCallee, 
              enumerate, bulletIndex, enumerationOffset,
              minimumSpacerSize, nolog, shortVersion
            |
    var  title  = this.classname.toUpper;

    var  maxKeyLength = "normalizeToRemainingRange".size;

    var  enviroA1, enviroA2, enviroA3, enviroA4, enviroA5,
         enviroB1, enviroB2, enviroB3,
         enviroC1,
         enviroArray;

    //
    if (this.isAlreadyReleased, { ^nil; });


    //
    enviroA1 = (
      value:                    this.value,
    );

    enviroA2 = (
      range:                    range.pretty(depth:0),
    );

    enviroA3 = (
      mul:                      mul,
    );

    enviroA4 = (
      add:                      add,
      curveType:                curveType,
      curveDirection:            curveDirection,
    );

    enviroA5 = (
      durationInSeconds:        durationInSeconds,
    );

                //SPACER

    enviroB1 = (
      activateUponAssignment:     activateUponAssignment,
      normalizeToRemainingRange:  normalizeToRemainingRange,
    );

    enviroB2 = (
      server:                   server.name,
    );

    enviroB3 = (
      isReleased:               isReleased,
    );

                //SPACER

    enviroC1 = (
      bus:                      bus,
      group:                    group.pr(nolog:true).minel(elisionLength:128),
      synth:                    synth.pr(nolog:true).minel(elisionLength:128),
    );


    //
    enviroArray = [ enviroA1, enviroA2, enviroA3, enviroA4, enviroA5, (),
                    enviroB1, enviroB2, enviroB3, (), 
                    enviroC1,
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


  //------------------------ -o-
  *pretty  { | pattern, elisionLength, depth,
               casei, compileString, indent, initialCallee, 
               enumerate, bulletIndex, enumerationOffset,
               minimumSpacerSize, nolog, shortVersion
             |
    var  title  = this.classname.toUpper;

    var  maxKeyLength = "curveDirectionEnum".size;

    var  enviroA1, enviroA2, 
         enviroB,
         enviroArray;


    //
    enviroA1 = (
      curveDirectionEnum:    curveDirectionEnum.pr(depth:0),
    );

    enviroA2 = (
      curveTypeEnum:        curveTypeEnum.pr(depth:0),
    );

        //SPACER

    enviroB = (
      group:                group.pretty(nolog:true).minel(elisionLength:128),
    );


    enviroArray = [ enviroA1, enviroA2, (),
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


}  //Curve

