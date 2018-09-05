//
// LongSample.sc
//
// Load and manipulate samples of long duration.  
// Long generally meaning longer than a few seconds, definately longer 
//   than grain-like sounds.
//
//
// This class emulates a particular technique of spatialization and
// sound deconstruction developed in the performance practice of Michael 
// Schumacher.  LongSample iterates through a given sample file according
// a variety of playback disciplines (eg: \irwin).  Once the playback index
// is set, the sample plays for a short period of time on one 
// channel while optionally echoing the same section on a second channel.  
// Signals to each channel are passed through different bandpass filters.
// This process repeats as long as desired, easilly allowing stops/starts 
// and many other changes to the minutae of playback index management and
// signal processing, all of which may be run automatically or individually 
// arrested in order to effect specific changes.
//
// As of the first release, this class also serves as a demonstration of
// SuperCollider classes and extentions defined by MobileSound, which aim
// to simultaneously manage a large number of Synth objects, while 
// minimizing loss of compute and architectural resources and to provide
// macro and micro level controls over each element of a performance 
// in a manner that engages intution and musical response via clear 
// feedback and a heirarchy of managable controls.  
// 
//
// 
// SUGGESTED USAGE
//   . Store soundfiles in LongSample.workingDirectory.
//
//   . Each instance of LongSample supports a single soundfile;
//       To manage multiple soundfiles, use one instance per soundfile.
//
//   . Use <instance>.release() (or LongSample.releaseAll()) to 
//       free system resources when finished.
//
//   . Calling LoadSample.init() once is required before the first 
//       instantiation in a given Server session.
//
//   . Large soundfiles may take time to load, measured in seconds.
//       Use <instance>.isReady() (or LongSample.isReadyAll()) to test
//       for load completion.  See also BufferCache.demo().
//
//   . Introduce a small delay between each instantiation when multiple
//       instances are created in blocks.
//
//   . Use <instance>.runStop() (or LongSample.runStopAll()) to
//       toggle between run (play) and stopped states.
//
//   . See <instance>.pr(r)etty() for feedback and other controls.
//     Run the LongSample.demo().
//
//
//
// INITIALIZATION SEQUENCE
//   . All Synths except synthPlayback created during library initializtion.
//
//   . Initialize soundfile Buffer and load entire buffer into memory.
//
//   . Create and define all IterationParameter variables.
//
//   . Create "synth graph second half"...
//       + Create group instance for LongSample instance on 
//           Server.defaultGroup, lazily move into place once 
//           group heirarchy is created;
//       + Establish/create heirarchy of MOS groups for this class;
//       + Create groups and initialize instances for first and 
//           second channels.
//
//   . Create "synth graph first half"...
//       + Define synthPlayback once soundfile Buffer is ready,
//           use the number of Buffer channels in the Synth name;
//       + Create synthPlayback and create an OSCdef to watch it.
//       + Register this instance in playbackSynthInstanceDictionary.
//
//
//
// INTERNAL STATE
//   Test operational state via isReady, isRunning, isReleased--
//
//     . isReady() means the instance is available to .run().
//         (Internally: synthPlaybackName.notNil)
//
//     . isRunning means .run() is active (versus being .stop()'ed).  
//         .run() initiates a looping cycle of (short) playing and 
//         sleeping states.
//
//     . isReleased() means all instance resources have been 
//         reclaimed by the system.  Such an instance will not run again.
//         (Internally: group.isNil)
//
// See below for the list of public methods.  There are additional ways
// to cheat if you look under the hood, though such usage is not supported.
// For example... using .getPlaybackIndex() to set .playbackIndexInFrames.
// This takes a second and first sets .playbackIndexInFramesRaw.
// The process is complete when .playbackIndexInFramesRawIsReady = true.
//
//
//
// SCLANG MOVES FASTER THAN IT THINKS
//   LongSample encourages bulk operations by simultaneously instantiating 
//   multiple instances.  However, this can cause trouble in, at least, two 
//   cases:
//     1) First time creation of group heirarchy; 
//     2) Registering Buffer instances in BufferCache.  
//
//   LongSample.init() will handle the first case.  To prevent the second
//   case, use a short delay (~0.1 second) between instantiations.
//
//   The class is robust enough to function consistently without these
//   precautions, though the supporting infrastructure may generate
//   messes (case #1) or inconsistencies (case #2).
//
//   In general, this will be TRUE OF ANY SET OF OPERATIONS that occur 
//   very quickly and in large batches because sclang does not have 
//   means to gracefully handle multiple simultaneous threads (forks) 
//   that vie for common resources.
//
//
//
// METHODS THAT CHANGE STATE OF synthPlayback
//   ampMaster_()
//   startPlaybackSynth()
//   stopPlaybackSynth()
//   getPlaybackIndex()
//   setPlaybackIndex()
//
// Other synths are set by initLongSample() and via setter methods
// with similar names to the synths, most of which define 
// InstanceParameters.
//
//
//
// USE OF SPINLOCK
//
// LongSample contains four asynchronous, dependency relationships:
// 
// During initialization...
// 
//   1  buildSynthGraphSecondHalf() WAITS for group.isPlaying(),
//        where group is SETUP by MobileSound.createGroup().
// 
//   2  createPlaybackSynth() WAITS for synthPlaybackName, 
//        which is SET by soundfilePathname_() 
//        in the completion function of Buffer.load().
// 
// During runtime...
// 
//   3  getPlaybackIndex() requests update from synthPlayback
//        and WAITS for playbackIndexInFramesRaw, 
//        which is SET by OSCdef (see buildSynthGraphFirstHalf());
//      then calls setPlaybackIndex() which SETS playbackIndexInFrames 
//        and playbackIndexInFramesIsReady.
// 
//   4  run() WAITS for playbackIndexInFramesIsReady.
// 
// 
//
// ALL VARIABLES, INCLUDING...
//   . Class variables
//   . Instance variables
//       + Public variables
//           * Initialization variables
//           * IterationParameter (smart!) variables
//       + Private operational parameters
// 
//   ...See declaration code section below.
// 
// 
// PUBLIC METHODS--
//   *usage  
//   *demo
// 
//   *new  *instanceAt  
//   isReady  (*)isReadyAll  
//   (*)pretty  
// 
//   run  stop  runStop  
//   (*)runStopAll  
//   iterate  iterateNow  
//   manualOn  manualOff  
// 
//   release  (*)releaseAll  
//   isReleased  
// 
// 
// PRIVATE METHODS--
//   *init  
// 
//   *initClass  
//   initLongSample  initLongSampleFinishAsync  
//   *createSynthDefs  
// 
//   buildSynthGraphFirstHalf 
//   buildSynthGraphSecondHalf
//   createPlaybackSynth  
// 
//   startPlaybackSynth  stopPlaybackSynth  
//   sendPlaybackSynthControlsWithGateResets  
// 
//   getPlaybackIndex  setPlaybackIndex  
//   setPlaybackIndexPerPlaybackStrategy  
//   eventTimeInMilliseconds  
// 
//   *operationOnAllInstances  
// 
// 
// SC PROTOCOL METHODS--
//   instanceArgs  
//   printClassNameOn  
//   storeParamsOn  
//   storeArgs  
// 
// 
// MOS PROTOCOL SUPPORT--
//   (*)mosVersion 
// 
//
//
// SYNTH DEFINITIONS--
//   \mosSynthBandpassFilter
//   \mosSynthDelayL
//   \mosSynthRouteInToOut
//
//   \mosSynthPlayMonoFromNNNChannelBufferOutTwo
//      Where NNN == number of channels in buffer for this instance.
//
//
// DEPENDENCIES--
//   Buffer_MOS
//   BufferCache
//   Date_MOS
//   IterationParameter
//   Log 
//   Object_MOS
//   Parse
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

LongSample : Schumacher
{
  classvar    classVersion = "0.1";   //RELEASE

  classvar    invocationArgs;



  // Class variables.
  //
  classvar  <  baseDirectory,
            <  workingDirectory;

  classvar  <  playbackStrategies;                 // Array of Symbols.
  classvar  <  delayTimesInSecondsArray;           // Array of Numbers.
  classvar  <  millisecondDurationsArray;          // Array of Integers.
  classvar  <  bandpassFilterFrequencyPairsArray;  // Array of pair Arrays 
                                                   //   of Numbers.

  classvar  <  playbackSynthInstanceDictionary,
            <> psid;                            //ALIAS
                   // Dictionary of Classes (LongSample instance) 
                   //   keyed by Symbols (playbackSynth.nodeID).



  // Initialization variables.
  //
  var  <  soundfilePathname,
       <> soundfileBuffer,                      //STATUS
       <  soundfileBufferLoadWaitIterationMax;

  var  <  playbackStrategy,
          playbackStrategyPrevious,
       <  playbackStrategyIrwinMultiple;

  var  <  eventTimeMultiplier,
       <  eventTimeMultiplierSleepRatio;

  var     ampMasterShadow,          
       <  ampChannels; 
          //ALSO: ampMaster

  var  <  allowSetParameterBetweenIterations,
       <  isManualBecomesAutomaticallyActive; 

  var  <  server;


  // IterationParameters.
  //
  var  <  outputBusOrBusses,                    // Array of 1-2 Integers
       <  channel1BandpassFilterFrequencyPair,  // Array of 1-2 Numbers
       <  channel2BandpassFilterFrequencyPair,  // Array of 1-2 Numbers
       <  channel2DelayTimeInSeconds;           // Number

  var  <> arrayOfIterationParameters;


  // Operational variables.
  //
  var  <> group;                        //SPINLOCK TARGET; STATUS

  var  <> group1Channel,
       <> synth1BandpassFilter,
       <> synth1Route,

       <> group2Channel,
       <> synth2BandpassFilter,
       <> synth2Delay,
       <> synth2Route;

  var  <  lowestFrequencyForBandpassFilter,
          highestFrequencyForBandpassFilter;

  var     audioBus1,
          audioBus2,
          unusedAudioBus;

  var  <> synthPlayback,
          synthPlaybackName;            //SPINLOCK TARGET; STATUS

  var  <> playbackIndexInFrames,
       <> playbackIndexInFramesRaw,             //SPINLOCK TARGET
       <> playbackIndexInFramesIsReady,         //SPINLOCK TARGET

       <  playbackIndexQueryWaitIteration,
       <  playbackIndexQueryWaitIterationMax;

  var     eventTimeInMillisecondsShadow,
          eventTimeInMillisecondsShadowLabel;

  var     playFunction,
          sleepFunction,
          currentRunSequenceNumber,
       <  isRunning;

  var  <  soundfileBasename;

  var  <  verbose;



  //---------------------------------------------------------- -o--
  *mosVersion { ^super.mosVersion(classVersion); }
  mosVersion  { ^this.class.mosVersion; }




  //---------------------------------------------------------- -o--
  // Lifecycle, constructors, class memory.

  //------------------------ -o-
  *initClass  
  {
    Log.info(thisFunction).postln; 

    invocationArgs = "soundfilePathname, [playbackStrategy, eventTimeMultiplier, eventTimeMultiplierSleepRatio, ampMaster, ampChannels, isManualBecomesAutomaticallyActive, allowSetParameterBetweenIterations, outputBusOrBusses, channel1BandpassFilterFrequencyPair, channel2BandpassFilterFrequencyPair, channel2DelayTimeInSeconds, soundfileBufferLoadWaitIterationMax, server]"; 

    //
    millisecondDurationsArray = [ 17, 23, 29, 31, 37, 43 ];

    delayTimesInSecondsArray = [
        0.0, 0.1, 0.5, 
        1, 2, 3, 7, 
        13, 23, 37, 53, 67, 91, 
        123, 157, 203, 271, 373, 
      ];

    bandpassFilterFrequencyPairsArray = [
        [ 20,   20480 ],
        [ 40,   20480 ],
        [ 80,   20480 ],
        [ 160,  20480 ],
        [ 320,  20480 ],
        [ 640,  20480 ],
        [ 1280, 20480 ],
        [ 2560, 20480 ],

        [ 20,   10240 ],
        [ 40,   10240 ],
        [ 80,   10240 ],
        [ 160,  10240 ],
        [ 320,  10240 ],
        [ 640,  10240 ],
        [ 1280, 10240 ],
        [ 2560, 10240 ],

        [ 20,   5120 ],
        [ 40,   5120 ],
        [ 80,   5120 ],
        [ 160,  5120 ],
        [ 320,  5120 ],
        [ 640,  5120 ],
        [ 1280, 5120 ],
        [ 2560, 5120 ],

        [ 20,   2560 ],
        [ 40,   2560 ],
        [ 80,   2560 ],
        [ 160,  2560 ],
        [ 320,  2560 ],
        [ 640,  2560 ],
        [ 1280, 2560 ],

        [ 20,  1280 ],
        [ 40,  1280 ],
        [ 80,  1280 ],
        [ 160, 1280 ],
        [ 320, 1280 ],
        [ 640, 1280 ],

        [ 20,  640 ],
        [ 40,  640 ],
        [ 80,  640 ],
        [ 160, 640 ],
        [ 320, 640 ],
        [ 640, 640 ],
      ];

    playbackStrategies = [ 
                           \irwin,

                           \playFromBeginning, 
                           \playFromLastIndex, 
                           \playFromRandomIndex,

                           \resetToBeginning,
                           \resetToRandomIndex,
                         ];

    //
    baseDirectory     = this.classname.toLower;
    workingDirectory  = Schumacher.workingDirectory +/+ baseDirectory;

    playbackSynthInstanceDictionary = Dictionary();
    psid = playbackSynthInstanceDictionary;   //ALIAS


    //
    StartUp.add  {
      File.createDirectory(workingDirectory);   //XXX  Best effort!
      this.createSynthDefs();
    };

  } // *initClass


  //------------------------ -o-
  // PROBLEM:  No simple way to proactively create 
  //   LongSample.superclass.group when many instances are 
  //   created simultaneously.  (Roughly three or more.) 
  // 
  // In particualar, when MobileSound.createGroup() is run many times 
  //   "simultaneously," it generates as many groups at each level of the
  //   heirarchy, which are orphaned and left behind, ignored by proper 
  //   cleanup of the instance that invoked them.
  //
  *init  {
    if (LongSample.superclass.group.isNil, {
      LongSample.superclass.createGroup();
    });
    
    ^'';
  }


  //------------------------ -o-
  *new  { | soundfilePathname, 
            playbackStrategy, 
            eventTimeMultiplier, 
            eventTimeMultiplierSleepRatio,
            ampMaster, 
            ampChannels, 
            allowSetParameterBetweenIterations, 
            isManualBecomesAutomaticallyActive,
            outputBusOrBusses,
            channel1BandpassFilterFrequencyPair, 
            channel2BandpassFilterFrequencyPair,
            channel2DelayTimeInSeconds, 
            soundfileBufferLoadWaitIterationMax,
            server
          |
    ^super.new.initLongSample( soundfilePathname, 
                               playbackStrategy, 
                               eventTimeMultiplier, 
                               eventTimeMultiplierSleepRatio,
                               ampMaster, 
                               ampChannels, 
                               allowSetParameterBetweenIterations, 
                               isManualBecomesAutomaticallyActive,
                               outputBusOrBusses,
                               channel1BandpassFilterFrequencyPair, 
                               channel2BandpassFilterFrequencyPair,
                               channel2DelayTimeInSeconds, 
                               soundfileBufferLoadWaitIterationMax,
                               server
                             );
  } // *new


  //------------------------ -o-
  // NB  Indicators.  Whether...
  //       ...group is defined indicates if instance is active,
  //            otherwise it has been released.
  //       ...synthPlayback is defined indicates if instance can be run,
  //            otherwise it is not ready to run.
  //
  // NB  Only the Buffer, Busses, Nodes and the OSCdef are freed and cleared.
  //
  release  { | force
             |
    var  instanceID  = nil;


    //DEFAULTS.
    force = force ?? false;

    if (Parse.isInstanceOfClass(thisFunction, 
                  force, "force", Boolean).not, { ^nil; });


    //
    if (force.not && this.isReleased(), { ^nil; });


    //
    if (isRunning, { this.stopPlaybackSynth(); });
    isRunning = false;


    if (synthPlayback.notNil, { 
      instanceID = synthPlayback.nodeID.asSymbol; 
      this.class.playbackSynthInstanceDictionary[instanceID] = nil;
    });

    OSCdef(synthPlaybackName.asSymbol).free;

    if (soundfileBuffer.notNil, {
      soundfileBuffer.releaseBufferCacheEntry();
      soundfileBuffer = nil;                            //STATUS
    });


    audioBus1.free;
    audioBus1 = nil;

    audioBus2.free;
    audioBus2 = nil;

    group.deepFree;
    group.free;
    group = nil;                        //SPINLOCK TARGET; STATUS

    synthPlayback         = nil;
    synthPlaybackName     = nil;        //SPINLOCK TARGET; STATUS

    group1Channel         = nil;
    synth1BandpassFilter  = nil;
    synth1Route           = nil;

    group2Channel         = nil;
    synth2BandpassFilter  = nil;
    synth2Delay           = nil;
    synth2Route           = nil;


    //
    ^nil;
  }

  //
  *releaseAll  { ^this.operationOnAllInstances(\release); }
  releaseAll   { ^this.class.releaseAll(); }


  //------------------------ -o-
  // group is the root of the node graph for this object.
  //
  // NB  Let the status of group govern the status of the object.
  //
  // NB  ALSO TEST for weird err conditions...
  //
  isReleased  { | verbose                               //LOGS
                |
    var  thisInstanceIsReleased  = group.isNil;         //STATUS


    //DEFAULTS.
    verbose = verbose ?? true;

    if (Parse.isInstanceOfClass(thisFunction, 
                  verbose, "verbose", Boolean).not,  { ^nil; });


    //
    if (thisInstanceIsReleased, {
      if (verbose, {
        Log.error( thisFunction, 
                   "[%]  This instance of % has been RELEASED.", 
                      soundfileBasename, this.classname
                 ).postln;  
      });

      ^thisInstanceIsReleased;
    });


    //
    if (this.soundfileBuffer.notNil, {
      if (this.isReady() && this.soundfileBuffer.isUndefined, 
      {
        Log.error(thisFunction, 
              "[%]  This instance HAS AN UNDEFINED Buffer.  Releasing...",
                 soundfileBasename
           ).postln; 

        this.release();
        thisInstanceIsReleased = true;
      });
    });


    //
    ^thisInstanceIsReleased;
  }


  //------------------------ -o-
  // synthPlaybackName is defined once the Buffer is loaded.
  //
  isReady  { | verbose                                  //LOGS
             |
    var  thisInstanceIsReady  = synthPlaybackName.notNil;   //STATUS


    //DEFAULTS.
    verbose = verbose ?? true;

    if (Parse.isInstanceOfClass(thisFunction, 
                  verbose, "verbose", Boolean).not,  { ^nil; });


    //
    if (thisInstanceIsReady.not && verbose, {
      Log.error( thisFunction, 
                 "[%]  This instance of % is NOT YET READY.", 
                    soundfileBasename, this.classname
               ).postln;  
    });


    //
    ^thisInstanceIsReady;
  }


  //
  *isReadyAll  { ^this.operationOnAllInstances(\isReady); }
  isReadyAll   { ^this.class.isReadyAll(); }




  //------------------------ -o-
  // DEFAULTS.
  //
  initLongSample  { | soundfilePathname, 
                      playbackStrategy, 
                      eventTimeMultiplier, 
                      eventTimeMultiplierSleepRatio,
                      ampMaster, 
                      ampChannels, 
                      allowSetParameterBetweenIterations, 
                      isManualBecomesAutomaticallyActive,
                      outputBusOrBusses,
                      channel1BandpassFilterFrequencyPair, 
                      channel2BandpassFilterFrequencyPair,
                      channel2DelayTimeInSeconds, 
                      soundfileBufferLoadWaitIterationMax,
                      server
                    |                           //POSTS; FORKS (via callees)
    var  numOutputBusChannels;


    //
    if (this.class.superclass.group.isNil, {
      Log.error(thisFunction, 
                  "MUST RUN %.init() before creating % instances.", 
                     this.classname, this.classname
               ).postln; 

      ^nil;
    });



    // Initialization variables.
    //

    //NB  synthPlaybackName is defined in this.soundfilePathname.
    //
    synthPlaybackName       = nil;                     //SPINLOCK TARGET
    soundfileBasename       = Object.labelUndefined;
    this.soundfilePathname  = soundfilePathname;

    this.soundfileBufferLoadWaitIterationMax = 
           soundfileBufferLoadWaitIterationMax ?? 30;


    this.playbackStrategy = playbackStrategy ?? \irwin;         //XXX

    playbackStrategyPrevious            = nil;
    this.playbackStrategyIrwinMultiple  = 1;


    this.eventTimeMultiplier  = eventTimeMultiplier  ?? 100;    //XXX
    this.eventTimeMultiplierSleepRatio  
                              = eventTimeMultiplierSleepRatio  ?? 1;


    this.ampMaster    = ampMaster    ?? 1.0;
    ampMasterShadow   = this.ampMaster;

    this.ampChannels  = ampChannels  ?? 1.0.dup;


    this.allowSetParameterBetweenIterations = 
                   allowSetParameterBetweenIterations ?? false;
    this.isManualBecomesAutomaticallyActive = 
                   isManualBecomesAutomaticallyActive ?? false;


    this.server = server ?? Server.default;



    // Operational variables.
    //
    group = nil;

    lowestFrequencyForBandpassFilter   = 5;             //XXX
    highestFrequencyForBandpassFilter  = 22000;         //XXX

    audioBus1       = Bus.audio(server);
    audioBus2       = Bus.audio(server);

    unusedAudioBus  = 9999;                             //XXX

    this.buildSynthGraphSecondHalf(); 


    //
    playbackIndexInFrames               = 0;
    playbackIndexInFramesRaw            = -1;
    playbackIndexInFramesIsReady        = true;

    playbackIndexQueryWaitIteration     = 0.001;        //XXX
    playbackIndexQueryWaitIterationMax  = 10.000;       //XXX


    //
    eventTimeInMillisecondsShadow       = 0;
    eventTimeInMillisecondsShadowLabel  = "play";

    playFunction              = nil;
    sleepFunction             = nil;
    currentRunSequenceNumber  = 0;
    isRunning                 = false;

    this.verbose = false;



    // IterationParameters.
    //
    //NB  MUST come after this.buildSynthGraphSecondHalf().
    //
    arrayOfIterationParameters  = Array();
    numOutputBusChannels        = this.server.options.numOutputBusChannels;

    this.outputBusOrBusses = outputBusOrBusses;

    this.channel1BandpassFilterFrequencyPair = 
                                      channel1BandpassFilterFrequencyPair;

    this.channel2BandpassFilterFrequencyPair = 
                                      channel2BandpassFilterFrequencyPair;

    this.channel2DelayTimeInSeconds = channel2DelayTimeInSeconds;



    //NB  Absentees include: group
    //
    if (    this.soundfilePathname.isNil 
         || this.soundfileBasename.isNil

         || this.soundfileBuffer.isNil
         || this.soundfileBufferLoadWaitIterationMax.isNil

         || this.playbackStrategy.isNil 
         || playbackStrategyPrevious.notNil
         || this.playbackStrategyIrwinMultiple.isNil

         || this.eventTimeMultiplier.isNil 
         || this.eventTimeMultiplierSleepRatio.isNil

         || this.ampMaster.isNil 
         || ampMasterShadow.isNil
         || this.ampChannels.isNil 

         || this.allowSetParameterBetweenIterations.isNil 
         || this.isManualBecomesAutomaticallyActive.isNil

         || this.server.isNil

                //
         || this.outputBusOrBusses.isNil
         || this.channel1BandpassFilterFrequencyPair.isNil 
         || this.channel2BandpassFilterFrequencyPair.isNil
         || this.channel2DelayTimeInSeconds.isNil 

                //
         || audioBus1.isNil
         || audioBus2.isNil

         || this.group1Channel.isNil
         || this.synth1BandpassFilter.isNil
         || this.synth1Route.isNil

         || this.group2Channel.isNil
         || this.synth2BandpassFilter.isNil
         || this.synth2Delay.isNil
         || this.synth2Route.isNil

                //
         || this.playbackIndexInFrames.isNil
         || this.playbackIndexInFramesRaw.isNil
         || this.playbackIndexInFramesIsReady.isNil

         || playbackIndexQueryWaitIteration.isNil
         || playbackIndexQueryWaitIterationMax.isNil

         || eventTimeInMillisecondsShadow.isNil
         || eventTimeInMillisecondsShadowLabel.isNil

         || playFunction.notNil
         || sleepFunction.notNil
         || currentRunSequenceNumber.isNil
         || isRunning.isNil

         || verbose.isNil
          ,
    {
      Log.error(thisFunction, 
            "[%]  Initialization FAILED.", soundfileBasename).postln;
      this.release(force:true);
      ^nil;
    });


 
    // Properly set parameters in synth graph previously created
    //   with mocked values.  Use InstanceParamater variables. 
    //
    this.synth1BandpassFilter.set(
      \freqLow,   this.channel1BandpassFilterFrequencyPair.get[0],
      \freqHigh,  this.channel1BandpassFilterFrequencyPair.get[1]
    );

    this.synth1Route.set(\outBus, this.outputBusOrBusses.get[0]);


    //
    if (this.outputBusOrBusses.size >= 2, 
    {
      this.synth2BandpassFilter.set(
        \freqLow,   this.channel2BandpassFilterFrequencyPair.get[0],
        \freqHigh,  this.channel2BandpassFilterFrequencyPair.get[1]
      );

      this.synth2Delay.set(\delayTime, this.channel2DelayTimeInSeconds);

      this.synth2Route.set(\outBus, this.outputBusOrBusses.get[1]);

      this.group2Channel.run(true);
    });


    //
    this.initLongSampleFinishAsync();


    //
    ^this;
  }


  //------------------------ -o-
  initLongSampleFinishAsync  
  {                                                     //FORKS
    var  instanceID  = nil;


    //
    {
      this.createPlaybackSynth();
      this.buildSynthGraphFirstHalf();

      //
      if (synthPlayback.isNil, {
        Log.error(thisFunction, 
              "[%]  Initialization (asynchoronous) FAILED.", soundfileBasename
            ).postln;

        this.release();

      }, {
        instanceID = synthPlayback.nodeID.asSymbol;
        this.class.playbackSynthInstanceDictionary[instanceID] = this;
      });

      '';
    }.fork;

    ^'';
  }



  //------------------------ -o-
  instanceArgs  { | asStringOrArray
                  |
    var  errorMessage  = "asStringOrArray MUST be \string or \array";


    //
    if (asStringOrArray.isNil, {
      ^Log.error(thisFunction, errorMessage);
    });


    //
    switch (asStringOrArray, 

      //
      \string, 
         { ^("%%%, %%, %, %, %, %, %, %, %, %, %, %, %, %")
               .format( 
                  $", soundfilePathname, $",
                  $\\, playbackStrategy, 
                  eventTimeMultiplier, 
                  eventTimeMultiplierSleepRatio,
		  this.ampMaster, 
                  ampChannels,
		  allowSetParameterBetweenIterations, 
		  isManualBecomesAutomaticallyActive,
                  outputBusOrBusses,
		  channel1BandpassFilterFrequencyPair,
		  channel2BandpassFilterFrequencyPair,
		  channel2DelayTimeInSeconds, 
                  soundfileBufferLoadWaitIterationMax,
                  server
	       );
	 },


      //
      \array,  
         {
           ^[ 
              soundfilePathname,
              playbackStrategy.pretty(), 
              eventTimeMultiplier, 
              eventTimeMultiplierSleepRatio,
              this.ampMaster, 
              ampChannels, 
              allowSetParameterBetweenIterations, 
              isManualBecomesAutomaticallyActive,
              outputBusOrBusses,
              channel1BandpassFilterFrequencyPair, 
              channel2BandpassFilterFrequencyPair,
              channel2DelayTimeInSeconds, 
              soundfileBufferLoadWaitIterationMax,
              server
            ];
         },


      //
      { ^Log.error(thisFunction, errorMessage); }

    );

  } //instanceArgs




  //---------------------------------------------------------- -o--
  // Class support methods.
  //
  // printClassNameOn generated by .asString.
  // storeParamsOn generated by .asCompileString.
  //

  //------------------------ -o-
  *usage  { ^("USAGE: %.new(%)").format(this.classname, invocationArgs); }

  //
  printClassNameOn  { |stream|  stream << ("%").format(this.classname); }

  //
  storeParamsOn  { |stream|                           
    stream << ("%.new(%)").format(this.classname, this.instanceArgs(\string));
  }

  //
  storeArgs  { |stream|  ^this.instanceArgs(\array); }




  //---------------------------------------------------------- -o--
  // Getters/setters.

  //------------------------ -o-
  // ASSUME  Buffer.load() never fails.
  //
  // DEFINE synth name "mosSynthPlayMonoFromNNNChannelBufferOutTwo".
  //        Where NNN is number of channels in the buffer.
  //
  soundfilePathname_  { | pathname
                        |
    var  synthPlaybackNameFunction  = nil;
    var  logContext                 = thisFunction;


    //NB  synthPlaybackName determined by number of channels in
    //    soundfileBuffer.
    //
    synthPlaybackNameFunction = { | logContext, buffer
                                  |
      synthPlaybackName = \mosSynthPlayMonoFrom
                             ++ buffer.numChannels 
                             ++ \ChannelBufferOutTwo;
    };


    //
    case
      //ERRORS.
      { soundfilePathname.notNil; }
          { Log.error( logContext, 
                       "% can only be defined ONCE DURING INITIALIZATION.", 
                          thisFunction.methodname
                     ).postln; }

      { Parse.isInstanceOfClass(thisFunction, 
                  pathname, thisFunction.methodname, String).not; }
          { /*EMPTY*/ }

      { pathname.isFile.not; }
          { Log.error( thisFunction, 
                       "% is NOT A FILE.  (%)", 
                          thisFunction.methodname, pathname
                     ).postln; }

      //SUCCESS.
      { 
        var  completionFunction;

        completionFunction = { | self, buffer
                               |
          if (buffer.numChannels > 1, 
          {
            Log.warning( logContext, 
                     "Audio file \"%\" contains MORE THAN ONE CHANNEL. (%)"
                  ++ "  Output will be summed to single channel.", 
                       buffer.path.basename, 
                       buffer.numChannels
                ).postln;
          });

          synthPlaybackNameFunction.(self, buffer);
        };


        //
        soundfilePathname = pathname; 
        soundfileBasename = pathname.basename;
                 
        this.soundfileBuffer = 
               Buffer.load( path:                soundfilePathname, 
                            server:              this.server, 
                            completionFunction:  completionFunction
                          );
        ^this;
      }

    ; //endcase

    ^nil;
  }
 

  //------------------------ -o-
  // Maximum duration to wait (in seconds) for Buffer to load soundfile.
  //
  soundfileBufferLoadWaitIterationMax_  { | numberValue
                                          |
    //
    if (Parse.isInstanceOfClass(thisFunction, 
                  numberValue, thisFunction.methodname, Number).not,  { ^nil; });

    // 
    if (numberValue < 0, 
    {
      Log.error(thisFunction, 
            "[%]  % MUST BE a Number greater than zero.  (%)", 
               soundfileBasename, thisFunction.methodname, numberValue
          ).postln; 

    }, {
      soundfileBufferLoadWaitIterationMax = numberValue;
      ^this;
    });


    //
    ^nil;
  }


  //------------------------ -o-
  playbackStrategy_  { | strategy
                       |
    case
      //ERRORS.
      { Parse.isInstanceOfClass(thisFunction,
                  strategy, thisFunction.methodname, Symbol).not;
      }
          { /*EMPTY*/ }

      { playbackStrategies.includes(strategy).not; }
          { Log.error( thisFunction, 
                       "[%]  UNKNOWN %.  (%)",
                         soundfileBasename, thisFunction.methodname, strategy
                     ).postln; }

      //SUCCESS.
      { 
        playbackStrategyPrevious = playbackStrategy;
        playbackStrategy = strategy; 

        ^this;
      }

    ; //endcase

    ^nil;
  }


  //------------------------ -o-
  eventTimeMultiplier_  { | value
                          |
    case 
      //ERRORS.
      { Parse.isInstanceOfClass(thisFunction,
                  value, thisFunction.methodname, Float).not;
      }
          { /*EMPTY*/ }

      { value <= 0; }
          { Log.error( thisFunction, 
                       "[%]  % MUST be a Float greater than zero.  (%)", 
                          soundfileBasename, thisFunction.methodname, value 
                     ).postln; }

      //SUCCESS.
      { eventTimeMultiplier = value; 
        ^this;
      }

    ; //endcase

    ^nil;
  }


  //------------------------ -o-
  eventTimeMultiplierSleepRatio_  { | number
                                    |
    case 
      //ERRORS.
      { Parse.isInstanceOfClass(thisFunction,
                  number, thisFunction.methodname, Number).not;
      }
          { /*EMPTY*/ }

      { number < 0; }
          { Log.error( thisFunction, 
                       "[%]  % MUST be a Number GREATER THAN ZERO.  (%)", 
                          soundfileBasename, thisFunction.methodname, number 
                     ).postln; }

      //SUCCESS.
      { eventTimeMultiplierSleepRatio = number; 
        ^this;
      }

    ; //endcase

    ^nil;
  }


  //------------------------ -o-
  // RETURN:  Current value: Float or current Bus value.
  //
  //NB  Shadow value allows shadowed value to behave the same
  //    regardless of its type. 
  //
  ampMaster  
  { 
    if (ampMasterShadow.isBus, {
      ^ampMasterShadow.getSynchronous().round(1e-12);
    }, {
      ^ampMasterShadow;
    });
  }


  //------------------------ -o-
  //NB  Bus enabled.  See Curve.
  //
  ampMaster_  { | floatOrCurve
                |
    var  ampMasterValue  = nil,
         busValueLabel   = "bus value: ";

                 
    //
    case
      //ERROR.
      { Parse.isInstanceOfClass(thisFunction, 
                  floatOrCurve, thisFunction.methodname, [Float, Curve]).not; 
      }
          { /*EMPTY*/ }

      {  
        if (floatOrCurve.isCurve(), {
          var  faderValue  = floatOrCurve.value();

          if (faderValue.isNil, { ^nil; });
          
          faderValue < 0;
        }, {
          busValueLabel = "";
          floatOrCurve < 0; 
        });
      }
          { Log.error( thisFunction, 
                       "[%]  % MUST be GREATER OR EQUAL to zero.  (%%)", 
                          soundfileBasename, 
                          thisFunction.methodname, 
                          busValueLabel,
                          if (floatOrCurve.isCurve, 
                                { floatOrCurve.value(); },
                                { floatOrCurve; } );
                     ).postln; }


      //SUCCESS.
      { 
        if (floatOrCurve.isCurve, 
        {
          if (ampMasterShadow.isBus(), {
            ampMasterValue = ampMasterShadow.getSynchronous();
          }, {
            ampMasterValue = ampMasterShadow;
          });

          ampMasterShadow = floatOrCurve.configurationComplete(
                                           ampMasterValue,
                                           [0.0, 1.0.max(ampMasterValue)]
                                         );
        }, {
          ampMasterShadow = floatOrCurve;
        });

        if (this.synthPlayback.notNil, {
          if (ampMasterShadow.isBus, {
            this.synthPlayback.map(\amp, ampMasterShadow);
          }, {
            this.synthPlayback.set(\amp, ampMasterShadow);
          });
        });

        ^this;
      }

    ; //endcase


    //
    ^nil;
  }


  //------------------------ -o-
  ampChannels_ { | floatOrArray
                 |
    case
      //ERROR.
      { Parse.isInstanceOfClass(thisFunction, 
                  floatOrArray, thisFunction.methodname, [Array, Float]).not; 
      }
          { ^nil; }


      //
      { floatOrArray.isFloat; }
          {
            if (floatOrArray < 0, {
              Log.error( thisFunction, 
                    "[%]  % value(s) must be GREATER OR EQUAL to zero.  (%)", 
                       soundfileBasename, thisFunction.methodname, 
                       floatOrArray
                  ).postln; 
              ^nil;

            }, {
              //SUCCESS: Float.
              ampChannels = [floatOrArray];
            });
          }

      { floatOrArray.size != 2; }
          { Log.error( thisFunction, 
                     "[%]  % MUST be a Float or an"
                  ++ "Float Array of 1 or 2 elements.  (%)",  
                        soundfileBasename, thisFunction.methodname, 
                        floatOrArray
                ).postln; 
            ^nil;
          }

      { (floatOrArray[0] < 0) || (floatOrArray[1] < 0); }
          { Log.error( thisFunction, 
                     "[%]  % Float value(s) MUST be "
                  ++ "GREATER OR EQUAL to zero.  (%)", 
                        soundfileBasename, thisFunction.methodname, 
                        floatOrArray
                ).postln; 
            ^nil;
          }


      //SUCCESS: Array of Floats.
      { ampChannels = floatOrArray; }

    ; //endcase


    //
    if (floatOrArray.size > 0, {
      this.synth1Route !?  this.synth1Route.set(\amp, floatOrArray[0]);

      if (2 == floatOrArray.size, {
        this.synth2Route !?  this.synth2Route.set(\amp, floatOrArray[1]);
      });
    });


    ^this;
  }


  //------------------------ -o-
  allowSetParameterBetweenIterations_  { | value
                                         |
    case
      //ERROR.
      { Parse.isInstanceOfClass(thisFunction, 
                  value, thisFunction.methodname, Boolean).not; 
      }
          { /*EMPTY*/ }

      //SUCCESS.
      { allowSetParameterBetweenIterations = value; 
        ^this;
      }

    ; //endcase

    ^nil;
  }


  //------------------------ -o-
  isManualBecomesAutomaticallyActive_  { | value
                                         |
    case
      //ERROR.
      { Parse.isInstanceOfClass(thisFunction, 
                  value, thisFunction.methodname, Boolean).not; 
      }
          { /*EMPTY*/ }

      //SUCCESS.
      { isManualBecomesAutomaticallyActive = value; 
        ^this;
      }

    ; //endcase

    ^nil;
  }


  //------------------------ -o-
  // value may be passed as Integer or Array of one or two Integers.  
  //   Internally, value is always represented as an Array of Integer(s).
  //   (Cf. parseValueFunctionString.)
  //
  outputBusOrBusses_  { | value                         //ITERATION PARAMETER
                        |
    var  parseValueFunctionString,
         valueResetFunctionString,
         iterateFunctionString;


    // Define InstanceParameter functions.
    //
    parseValueFunctionString = { | self, refInst, incomingValue
                                 |
      var  numOutputBusChannels  = refInst.server.options.numOutputBusChannels,
           validatorFunctionString;

      //
      validatorFunctionString = { | logContext, 
                                    value, valueName, 
                                    classRequirement,
                                    validatorContextArray
                                  |
        var  numOutputBusChannels  = validatorContextArray[0];
                 
        if (value.class == classRequirement, {
          value.inRange(0, numOutputBusChannels - 1);
        }, {
          false;
        });
      }.cs;


      //
      self.newValue = nil;

      if (incomingValue.isNil, {
        incomingValue = self.valueResetFunction.().(self, refInst);
      });

      if (Parse.isInstanceOfArrayOfTypeWithSize(self.parameterLogContext,
                    incomingValue, self.parameterName, 
                    Integer, 
                    validatorFunctionString:validatorFunctionString,
                    validatorContextArray:[numOutputBusChannels],
                    maxSize:2
                ), 
      {
        self.newValue = incomingValue.intoArray;
      });


      //
      self.newValue.notNil;
    }.cs;


    //
    valueResetFunctionString = { | self, refInst
                                 |
      var  numOutputBusChannels  = refInst.server.options.numOutputBusChannels,
           result                = nil;


      // NB  Always return an array, whether one or two elements.
      //
      result = 
        [ [numOutputBusChannels.rand], numOutputBusChannels.dup.rand ].choose;

      if (2 == result.size, 
      {
        while ({ result[0] == result[1]; }, {  //ASSUME it will terminate...
          result = numOutputBusChannels.dup.rand;
        });
      });
                 

      //
      result;
    }.cs;


    //
    iterateFunctionString = { | self, refInst
                              |
      refInst.synth1Route.set(\outBus, self.value[0]);

      if (self.value.size <= 1, { 
        refInst.group2Channel.run(false);

      }, {
        refInst.synth2Route.set(\outBus, self.value[1]);
        refInst.group2Channel.run(true);
      });

      '';
    }.cs;



    // Declare and set InstanceParameter.
    //
    if (outputBusOrBusses.isNil, 
    {
      outputBusOrBusses = IterationParameter(
        thisFunction,
        Array,
        this,
  
        parseValueFunctionString,
        valueResetFunctionString,
        iterateFunctionString,

        referringVariables: [ 
                              \server,
                              \group2Channel,
                              \synth1Route,
                              \synth2Route,
                            ]
      );

      arrayOfIterationParameters = 
        arrayOfIterationParameters.add(this.outputBusOrBusses);
    });


    //
    outputBusOrBusses.set(value);

    ^this;

  } //outputBusOrBusses_


  //------------------------ -o-
  channel1BandpassFilterFrequencyPair_  { | value
                                          |             //ITERATION PARAMETER
    var  parseValueFunctionString,
         valueResetFunctionString,
         iterateFunctionString;


    // Define InstanceParameter functions.
    //
    parseValueFunctionString = { | self, refInst, incomingValue
                                 |
      var  validatorFunctionString;


      //
      validatorFunctionString = { | logContext, 
                                    value, valueName, 
                                    classRequirement,
                                    validatorContextArray
                                  |
        var  lowestFrequencyForBandpassFilter  = validatorContextArray[0],
             soundfileBasename                 = validatorContextArray[1];

        var  rval  = (value >= lowestFrequencyForBandpassFilter);
        
        //
        if (rval.not, {
          Log.error( logContext, 
                     "[%]  Array elements MUST BE greater than %.  (%=%)",
                        soundfileBasename, 
                        lowestFrequencyForBandpassFilter, 
                        valueName, value.prettyShort
                   ).postln; 
        });

        rval;
      }.cs;


      //
      self.newValue = nil;

      if (incomingValue.isNil, {
        incomingValue = self.valueResetFunction.().(self, refInst);
      });

      if (Parse.isInstanceOfArrayOfTypeWithSize(self.parameterLogContext,
                    incomingValue, self.parameterName, 
                    Number, 
                    validatorFunctionString:validatorFunctionString,
                    validatorContextArray:
                      [ refInst.lowestFrequencyForBandpassFilter,
                        refInst.soundfileBasename
                      ],
                    size:2
                ), 
      {
        if (incomingValue[0] > incomingValue[1], {
          Log.error( thisFunction, 
                     "[%]  Low Frequency (%) MUST BE less than or equal to "
                        ++ "High Frequency (%).", 
                          refInst.soundfileBasename,
                          incomingValue[0].prettyShort,
                          incomingValue[1].prettyShort
                   ).postln;
                 
        }, {
          self.newValue = incomingValue;
        });
      });


      //
      self.newValue.notNil;
    }.cs;


    //
    valueResetFunctionString = { | self, refInst
                                 |
      refInst.class.bandpassFilterFrequencyPairsArray.choose;
    }.cs;


    //
    iterateFunctionString = { | self, refInst
                              |
      refInst.synth1BandpassFilter.set(
                \freqLow,   self.value[0],
                \freqHigh,  self.value[1]
              );

      '';
    }.cs;



    // Declare and set InstanceParameter.
    //
    if (channel1BandpassFilterFrequencyPair.isNil, 
    {
      channel1BandpassFilterFrequencyPair = IterationParameter(
        thisFunction,
        Array,
        this,
  
        parseValueFunctionString,
        valueResetFunctionString,
        iterateFunctionString,

        referringVariables: [ 
                              \lowestFrequencyForBandpassFilter,
                              \soundfileBasename,
                              \synth1BandpassFilter,
                            ]
      );

      arrayOfIterationParameters = 
                        arrayOfIterationParameters
                          .add(this.channel1BandpassFilterFrequencyPair);
    });


    //
    channel1BandpassFilterFrequencyPair.set(value);

    ^this;

  } //channel1BandpassFilterFrequencyPair_


  //------------------------ -o-
  channel2BandpassFilterFrequencyPair_  { | value
                                          |             //ITERATION PARAMETER
    var  parseValueFunctionString,
         valueResetFunctionString,
         iterateFunctionString;


    // Define InstanceParameter functions.
    //
    // XXX  parseValueFunctionString and valueResetFunctionString
    //      are identical to those in channel1BandpassFilterFrequencyPair.
    //
    parseValueFunctionString = { | self, refInst, incomingValue
                                 |
      var  validatorFunctionString;


      //
      validatorFunctionString = { | logContext, 
                                    value, valueName, 
                                    classRequirement,
                                    validatorContextArray
                                  |
        var  lowestFrequencyForBandpassFilter  = validatorContextArray[0],
             soundfileBasename                 = validatorContextArray[1];

        var  rval  = (value >= lowestFrequencyForBandpassFilter);
        
        //
        if (rval.not, {
          Log.error( logContext, 
                     "[%]  Array elements MUST BE greater than %.  (%=%)",
                        soundfileBasename, 
                        lowestFrequencyForBandpassFilter, 
                        valueName, value.prettyShort
                   ).postln; 
        });

        rval;
      }.cs;


      //
      self.newValue = nil;

      if (incomingValue.isNil, {
        incomingValue = self.valueResetFunction.().(self, refInst);
      });

      if (Parse.isInstanceOfArrayOfTypeWithSize(self.parameterLogContext,
                    incomingValue, self.parameterName, 
                    Number, 
                    validatorFunctionString:validatorFunctionString,
                    validatorContextArray:
                      [ refInst.lowestFrequencyForBandpassFilter,
                        refInst.soundfileBasename
                      ],
                    size:2
                ),
      {
        if (incomingValue[0] > incomingValue[1], {
          Log.error( thisFunction, 
                   "[%]  Low Frequency (%) MUST BE less than or equal to "
                ++ "High Frequency (%).", 
                      refInst.soundfileBasename,
                      incomingValue[0].prettyShort,
                      incomingValue[1].prettyShort
              ).postln;
                 
        }, {
          self.newValue = incomingValue;
        });
      });


      //
      self.newValue.notNil;
    }.cs;


    //
    valueResetFunctionString = { | self, refInst
                                 |
      refInst.class.bandpassFilterFrequencyPairsArray.choose;
    }.cs;


    //
    iterateFunctionString = { | self, refInst
                              |
      refInst.synth2BandpassFilter.set(
                \freqLow,   self.value[0],
                \freqHigh,  self.value[1]
              );

      '';
    }.cs;



    // Declare and set InstanceParameter.
    //
    if (channel2BandpassFilterFrequencyPair.isNil, 
    {
      channel2BandpassFilterFrequencyPair = IterationParameter(
        thisFunction,
        Array,
        this,
  
        parseValueFunctionString,
        valueResetFunctionString,
        iterateFunctionString,

        referringVariables: [ 
                              \lowestFrequencyForBandpassFilter,
                              \soundfileBasename,
                              \synth2BandpassFilter,
                            ]
      );

      arrayOfIterationParameters = 
                        arrayOfIterationParameters
                          .add(this.channel2BandpassFilterFrequencyPair);
    });


    //
    channel2BandpassFilterFrequencyPair.set(value);

    ^this;

  } //channel2BandpassFilterFrequencyPair_


  //------------------------ -o-
  channel2DelayTimeInSeconds_  { | value                //ITERATION PARAMETER
                                 |
    var  parseValueFunctionString,
         valueResetFunctionString,
         iterateFunctionString;


    // Define InstanceParameter functions.
    //
    parseValueFunctionString = { | self, refInst, incomingValue
                                 |
      var  isBadValue  = false;


      //
      self.newValue = nil;

      if (incomingValue.isNil, {
        incomingValue = self.valueResetFunction.().(self, refInst);
      });


      if (Parse.isInstanceOfClass(self.parameterLogContext, 
                    incomingValue, self.parameterName, Number).not, { 
        isBadValue = true;

      }, {
        if (incomingValue < 0, 
        {
          Log.error( self.parameterLogContext,  
                     "[%]  % MUST BE greater than zero.  (%)",
                        refInst.soundfilePathname.basename,
                        self.parameterName, incomingValue.prettyShort
                   ).postln; 

          isBadValue = true;
        });
      });


      if (isBadValue.not, {
        self.newValue = incomingValue;
      });


      //
      self.newValue.notNil;
    }.cs;


    //
    valueResetFunctionString = { | self, refInst
                                 |
      refInst.class.delayTimesInSecondsArray.choose;
    }.cs;


    //
    iterateFunctionString = { | self, refInst
                              |
      refInst.synth2Delay.set(\delayTime, self.value);

      '';
    }.cs;



    // Declare and set InstanceParameter.
    //
    if (channel2DelayTimeInSeconds.isNil, 
    {
      channel2DelayTimeInSeconds = IterationParameter(
        thisFunction,
        Number,
        this,
  
        parseValueFunctionString,
        valueResetFunctionString,
        iterateFunctionString,

        referringVariables: [ 
                              \synth2Delay,
                            ]
      );

      arrayOfIterationParameters = 
        arrayOfIterationParameters.add(this.channel2DelayTimeInSeconds);
    });


    //
    channel2DelayTimeInSeconds.set(value);

    ^this;

  } //channel2DelayTimeInSeconds_


  //------------------------ -o-
  server_  { | value
             |
    if (Parse.isInstanceOfClass(thisFunction, 
          value, thisFunction.methodname, Server).not,  { ^nil; });

    //
    server = value;

    ^this;
  }


  //------------------------ -o-
  playbackStrategyIrwinMultiple_  { | numberValue
                                    |
    if (Parse.isInstanceOfClass(thisFunction, 
          numberValue, "numberValue", Number).not,      { ^nil; });

    playbackStrategyIrwinMultiple = numberValue;

    ^this;
  }


  //------------------------ -o-
  verbose_  { | booleanValue
              |
    if (Parse.isInstanceOfClass(thisFunction, 
          booleanValue, "booleanValue", Boolean).not,   { ^nil; });

    verbose = booleanValue;

    ^this;
  }




  //---------------------------------------------------------- -o--
  // Public class methods.

  //------------------------ -o-
  // \mosSynthBandpassFilter
  // \mosSynthDelayL
  // \mosSynthRouteInToOut
  //
  *createSynthDefs  { | server
                      |
    //DEFAULTS.
    server = server ?? Server.default;


    // XXX  Setting freqLow=0 generates anomalies.  5 is close to zero.
    //
    SynthDef( \mosSynthBandpassFilter,  { | inBus=9999,  outBus=9999,  amp=1,
                                            freqLow=5,  freqHigh=999999
                                          |
        var  in  = In.ar(inBus),
             hpf, lpf;

        hpf = HPF.ar(in,  freqLow);
        lpf = LPF.ar(hpf, freqHigh);

        ReplaceOut.ar(outBus, lpf * amp);
      },

      rates: [ \ar, \ar, \kr, 0.200, 0.200 ]  
    ).store;


    //
    SynthDef( \mosSynthDelayL,  { | inBus=9999,  outBus=9999,  amp=1,
                                    delayTimeMax=10,  delayTime=0.5   //XXX
                                  |
        var  in = In.ar(inBus),
             delay;

        delay = DelayL.ar(in, delayTimeMax, delayTime);

        ReplaceOut.ar(outBus, delay * amp);
      }, 

      rates: [ \ar, \ar, 0.010, \kr, \kr ]
    ).store;


    //
    SynthDef( \mosSynthRouteInToOut,  { | inBus=9999,  outBus=9999,  amp=1
                                        |
        Out.ar(outBus, In.ar(inBus) * amp);
      },

      rates: [ \ar, \ar, 0.010 ]
    ).store;


    //
    ^'';

  } // *createSynthDefs


  //------------------------ -o-
  *instanceAt  { | playbackSynthInstanceIndex
                 |
    var  instance  = nil;

    //
    if (Parse.isInstanceOfClass(
                  thisFunction, 
                  playbackSynthInstanceIndex, "playbackSynthInstanceIndex", 
                  Integer
              ).not,  { ^nil; });

    //
    instance =
      playbackSynthInstanceDictionary[playbackSynthInstanceIndex.asSymbol];

    if (instance.notNil, { ^instance; });


    //
    Log.error( 
          thisFunction, 
          "CANNOT FIND playbackSynth at index %", playbackSynthInstanceIndex
        ).postln; 
        
    ^nil;
  }




  //---------------------------------------------------------- -o--
  // Public instance methods.

  //------------------------ -o-
  // Define co-routines playFunction and sleepFunction, both of
  // which are available for scheduling on SystemClock.
  //
  run                                   //SCHEDULES; FORKS (via functions)
  {                                                 
    var  runSequenceNumber  = currentRunSequenceNumber,
         spinlockRval       = nil,
         instanceID         = synthPlayback.nodeID.asSymbol;


    //
    if (this.isReleased(),   { ^nil; });
    if (this.isReady().not,  { ^nil; });

    if (isRunning, 
    {
      Log.warning(thisFunction, 
            "[%]  This instance of % is ALREADY RUNNING.", 
               soundfileBasename, this.classname).postln; 
      ^this;
    });


    //
    playFunction = { | time
                     |
      if (this.verbose, {
        Log.info(thisFunction, 
              ":: playFunction ::  instanceID=%  runSequenceNumber=%", 
                 instanceID, runSequenceNumber
            ).postln; 
      });

      //
      {
        this.setPlaybackIndexPerPlaybackStrategy();

        spinlockRval = 
          Routine.spinlock(
                       { playbackIndexInFramesIsReady; },
                    testFunctionString: 
                      "{ playbackIndexInFramesIsReady; }",

                    disallowExceptionError: true,
                    logContext:             thisFunction
                    //, verbose:              true
                  );

        if (spinlockRval < 0, {
          this.stop();

          Log.error(thisFunction, 
                   "Spinlock in playFunction FAILED.  ("
                ++ "playbackIndexInFramesIsReady=%)",
                      playbackIndexInFramesIsReady
              ).postln; 

        }, {
          this.iterate();
          this.startPlaybackSynth();

          //
          if (    isRunning 
               && (runSequenceNumber - currentRunSequenceNumber == 0), 
          {
            eventTimeInMillisecondsShadowLabel = "run";
            sleepFunction.systemSchedule(this.eventTimeInMilliseconds() / 1000);
          });

        });


        '';
      }.fork;

      nil;
    };


    //
    sleepFunction = { | time
                      |
      if (this.verbose, {
        Log.info(thisFunction, 
              ":: sleepFunction ::  instanceID=%  runSequenceNumber=%", 
                 instanceID, runSequenceNumber
            ).postln; 
      });

      if (isRunning, 
      {
        this.stopPlaybackSynth();

        if (playbackStrategy.intoArray.includes( \playFromLastIndex ), {
          this.getPlaybackIndex();
        });

        if ((runSequenceNumber - currentRunSequenceNumber) == 0, 
        {
          eventTimeInMillisecondsShadowLabel = "sleep";

          eventTimeInMillisecondsShadow = 
                                this.eventTimeInMilliseconds()
                              * this.eventTimeMultiplierSleepRatio;

          playFunction.systemSchedule(eventTimeInMillisecondsShadow / 1000);
        });
      });

      nil;
    };


    //
    isRunning = true;
    group.pauseContinue(true);

    playFunction.systemSchedule(nil);


    ^this;
  }


  //------------------------ -o-
  // ASSUME  Full cycle of runSequenceNumbers will always take longer
  //         than the longest run/sleep cycle.
  //
  // NB  Orphaned scheduled functions do not replicate.  Increasing 
  //       runSequenceNumber guarantees the pool of scheduled 
  //       functions will empty itself.
  //
  stop  
  {
    if (this.isReleased(),  { ^nil; });

    if (this.isReady().not, { ^nil; });

    if (isRunning.not, 
    {
      Log.warning(thisFunction, 
            "[%]  This instance of % is NOT RUNNING.", 
               soundfileBasename, this.classname).postln; 
      ^this;
    });


    //
    isRunning                 = false;
    currentRunSequenceNumber  = currentRunSequenceNumber + 1;   
                                            //NB  Okay if it wraps.
    this.stopPlaybackSynth();
    group.pauseContinue(false);


    //
    ^this;
  }


  //------------------------ -o-
  runStop  { | onOff
             |
    if (this.isReleased(),      { ^nil; });
    if (this.isReady().not,     { ^nil; });


    //DEFAULTS.
    onOff = onOff ?? isRunning.not;

    if (Parse.isInstanceOfClass(thisFunction, 
                  onOff, "onOff", Boolean).not, { ^nil; });

    //
    if (onOff, {
      this.run();
    }, {
      this.stop();
    });
    
    ^this;
  }

  //
  *runStopAll  { ^this.operationOnAllInstances(\runStop); }
  runStopAll   { ^this.class.runStopAll(); }


  //------------------------ -o-
  manualOn  {
    //
    if (this.isReleased(),      { ^nil; });
    if (this.isReady().not,     { ^nil; });

    //
    IterationParameter.setManualAllOn(arrayOfIterationParameters);
    ^this;
  }

  //
  manualOff  {
    //
    if (this.isReleased(),      { ^nil; });
    if (this.isReady().not,     { ^nil; });

    //
    IterationParameter.setManualAllOff(arrayOfIterationParameters);
    ^this;
  }


  //------------------------ -o-
  iterate  { | iterateNow
             |
    //
    if (this.isReleased(),      { ^nil; });
    if (this.isReady().not,     { ^nil; });


    //DEFAULTS.
    iterateNow = iterateNow ?? false;

    if (Parse.isInstanceOfClass(thisFunction, 
                  iterateNow, "iterateNow", Boolean).not,  { ^nil; });


    //
    if (iterateNow, {
      IterationParameter.iterateNowAll(arrayOfIterationParameters);
    }, {
      IterationParameter.iterateAll(arrayOfIterationParameters);
    });


    //
    ^this;
  }

  //
  iterateNow  { ^this.iterate(true); }




  //---------------------------------------------------------- -o--
  // Private class methods.

  //------------------------ -o-
  // RETURN:  
  //   For operation \isReady...
  //            true    on success,
  //            false   otherwise;
  //
  //   For all other operations...
  //            ''
  //
  *operationOnAllInstances  { | operation
                              |
    operation = operation.asString.toLower.asSymbol;


    //
    if (psid.isNil || (psid.size <= 0), { 
      if (\isready == operation, { ^false; }, { ^''; });
    });

    //
    switch (operation,
      //
      \isready,
        {
          psid.keys.do({ |key|
            if (psid[key].isReady().not, { ^false; });
          });

          ^true;
        },

      //
      \runstop,
        {
          psid.keys.do({ |key|
            psid[key].runStop();
          });
        },

      //
      \release,
        {
          psid.keys.do({ |key|
            psid[key].release();
          });
        },

      //
      { Log.error(thisFunction, 
                    "UNKNOWN switch value.  (%)", operation).postln;  
      }
    );

    ^'';
  }




  //---------------------------------------------------------- -o--
  // Private instance methods.

  //------------------------ -o-
  // ASSUME  We are running within a Routine.
  // ASSUME  createPlaybackSynth is called before this and has
  //           completed successfully.
  // ASSUME  For initial invocation, ampMasterShadow is always a
  //           Float, never a Bus.
  //
  buildSynthGraphFirstHalf                              //NEEDS ROUTINE
  {      
    if (synthPlaybackName.isNil, 
    {
      Log.error(thisFunction, "synthPlaybackName MUST be defined.").postln; 
      ^nil;
                 
    }, {
      this.synthPlayback = 
        Synth.newRegistered( 
                synthPlaybackName.asSymbol,
   
                [ \amp,                     ampMasterShadow,
                  \ampGate,                 0,
   
                  \outBus1,                 audioBus1,
                  \outBus2,                 audioBus2,
   
                  \buffer,                  this.soundfileBuffer,
                  \bufRdInterpolation,      2,
   
                  \startIndexInFrames,      0,
                  \endIndexInFrames,        this.soundfileBuffer.numFrames - 1,
   
                  \rateAndDirection,        1.0,
                  \rateAndDirectionGate,    0,
   
                  \queryPlaybackIndexGate,  0,
                  \resetIndexInFrames,      0,
                  \resetIndexInFramesGate,  0
                ],
   
                target:     this.group,
                addAction:  \addToHead
              );


      // Message format: [ oscPath, nodeID, -1, phasorIndex ]
      //
      OSCdef( this.synthPlayback.defName.asSymbol,  

              { | msg, time, replyAddr, recvPort
                |
                var  nodeID             = msg[1],
                     phasorIndex        = msg[3],
                     instance           = LongSample.instanceAt(nodeID),
                     soundfileBasename  = instance.soundfileBuffer.path.basename;

                instance.playbackIndexInFramesRaw = phasorIndex;

                if (instance.verbose, {
                  Log.info(thisFunction, 
                        "Phasor index (raw) is % (%s) for instance %/%.",
                        phasorIndex, 
                        instance.soundfileBuffer
                          .framesToSeconds(phasorIndex).roundPadRight(0.001),
                        nodeID, 
                        soundfileBasename
                      ).postln; 
                });
              }, 

              this.synthPlayback.oscPath(OSC.oscTokenBufferPlaybackIndex),

              argTemplate: [this.synthPlayback.nodeID]
            );
    });


    //
    ^this;
  } 


  //------------------------ -o-
  // Create top group for this instance.
  // Initiate group heirarchy for this suite.
  // When heirarchy is complete, move top group into place.
  //   (Meet fork with fork.)
  // Meanwhile, instantiate this instance.
  //
  // Instantiate Synths with generic values, because catch-22: 
  //   InstanceParameter variables need complete graph to execute checks 
  //   against variables of this instance.  InstanceParameter variables 
  //   will set proper values afterwards.  
  //
  // Top group is initially paused.
  // group2 is initially paused.
  // group1 is never paused.
  //
  // ASSUME  Synth graph is built only once.  
  //         New graphs require new instances.
  //
  buildSynthGraphSecondHalf                                     //FORKS
  {
    var  ampChannel2,
         outputChannel2,
         channel2IsPaused,
         rval  = -1;


    // Establish heirarchy of groups for this class.
    // Create group for this class and lazily reorder in heirarchy.
    //
    group = this.createGroup( target:   server.defaultGroup, 
                              server:   server, 
                              paused:   true
                              //, verbose:  true
                            );

    // NB  In practice, this generates orphaned groups when instantiating
    //     lots of instances at once.  As many orphaned groups at each
    //     level of the heirarchy as their are instantiations in the
    //     initial burst.  It works, but it cannot cleanup the orphans.
    //     Solution: LongSample.init().
    //
    this.class.superclass.createGroup(server:server, verbose:false);


    //
    {
      Routine.spinlock(    { this.class.superclass.group.isPlaying(); },
                        testFunctionString:     
                          "{ this.class.superclass.group.isPlaying(); }",
                        testIterationWait:      0.01,
                        logContext:             thisFunction
                        //, verbose: true
                      );

      if (this.soundfileBuffer.notNil, {
        if (    this.class.superclass.group.notNil
             && this.group.notNil, 
        {
          this.group.moveToHead(this.class.superclass.group);

        }, {
          Log.error( thisFunction, 
                "[%]  CANNOT MOVE %.group instance (%) into %.group (%).",
                     soundfileBasename,
                     this.classname, this.group,
                     this.class.superclass, this.class.superclass.group
              ).postln; 
        });
      });

      '';
    }.fork;



    // Build group for first channel.
    // ASSUME this.group has been created.
    //
    group1Channel = Group.newRegistered( target:     this.group, 
                                         addAction:  \addToTail,
                                         server:     server
                                       );
                                
    synth1BandpassFilter = 
      Synth.newRegistered( \mosSynthBandpassFilter,
                           [ \inBus,     audioBus1,
                             \outBus,    audioBus1,
                             \amp,       1,
            
                             \freqLow,   lowestFrequencyForBandpassFilter,
                             \freqHigh,  highestFrequencyForBandpassFilter,
                           ],
            
                           target:     group1Channel,
                           addAction:  \addToTail
                         );

    synth1Route = 
      Synth.newRegistered( \mosSynthRouteInToOut,
                           [
                             \inBus,   audioBus1,
                             \outBus,  unusedAudioBus,
                             \amp,     ampChannels[0],
                           ],

                           target:     group1Channel,
                           addAction:  \addToTail,
                         );


    // Build group for second channel.
    //
    if (ampChannels.size >= 2, {
      ampChannel2 = ampChannels[1];
    }, {
      ampChannel2 = ampChannels[0];
    });

    group2Channel = 
      Group.newRegistered( target:     this.group, 
                           addAction:  \addToTail, 
                           server:     server,
                           paused:     true
                         );

    synth2BandpassFilter = 
        Synth.newRegistered( \mosSynthBandpassFilter,
                             [ \inBus,     audioBus2,
                               \outBus,    audioBus2,
                               \amp,       1,

                               \freqLow,   lowestFrequencyForBandpassFilter,
                               \freqHigh,  highestFrequencyForBandpassFilter,
                             ],

                             target:     group2Channel,
                             addAction:  \addToTail
                           );

    synth2Delay = 
        Synth.newRegistered( \mosSynthDelayL,
                             [
                               \inBus,      audioBus2,
                               \outBus,     audioBus2,
                               \amp,        1,

                               \delayTimeMax,
                                  LongSample.delayTimesInSecondsArray.sort.last,
                               \delayTime,  0,
                             ],

                             target:     group2Channel,
                             addAction:  \addToTail,
                           );

    synth2Route = 
      Synth.newRegistered( \mosSynthRouteInToOut,
                           [
                             \inBus,   audioBus2,
                             \outBus,  unusedAudioBus,
                             \amp,     ampChannel2,
                           ],

                           target:     group2Channel,
                           addAction:  \addToTail,
                         );

    //
    ^this;

  }  //buildSynthGraphSecondHalf 


  //------------------------ -o-
  // synthPlaybackName is ready once Buffer is loaded and reflects the 
  //   number of channels in Buffer.
  // The Synth by this name uses soundFileBuffer to define the
  //   number of channels read by BufRd.
  //
  // NB  Does not matter if this runs multiple times.
  //
  // ASSUME  We are running within a Routine.
  //
  createPlaybackSynth  { | server                       //NEEDS ROUTINE
                         |
    var  phasorIndexMax  = (2 ** 24);   // From SuperCollider documentation...
    var  rval            = -1;


    //
    rval = Routine.spinlock( 
                                       { synthPlaybackName.notNil; },
             testFunctionString:      "{ synthPlaybackName.notNil; }",
             testIterationWaitMax:    soundfileBufferLoadWaitIterationMax,
             disallowExceptionError:  true,
             logContext:              thisFunction
             //, verbose:true
           );

    if (synthPlaybackName.isNil, {
      Log.error(thisFunction, 
            "[%]  FAILED TO CREATE synthPlaybackName within % seconds.", 
               soundfileBasename, rval.abs
          ).postln; 

      this.release(force:true);

      ^'';
    });


    /*------------------------------------------------------------------------
    // NOTE  This is not true in SuperCollider v3.6.6.
    //
    phasorIndexMax = (phasorIndexMax / soundfileBuffer.sampleRate).floor;

    if (phasorIndexMax < soundfileBuffer.duration, 
    {
      Log.warning(thisFunction,
               "[%]  soundfileBuffer is longer (%) than Phasor "
            ++ "can index (%) at Buffer sample rate (%).",
                  soundfileBasename,
                  Date.secondsToClocktime(soundfileBuffer.duration, 
                         printHours:false, clearLeadingZero:true),
                  Date.secondsToClocktime(phasorIndexMax, 
                         printHours:false, clearLeadingZero:true),
                  soundfileBuffer.sampleRate
         ).postln; 
    });
    ------------------------------------------------------------------------*/


    // To play whole buffer use (ASSUMING its within index limit of Phasor):
    //   startIndexInFrames  = 0
    //   endIndexInFrames    = (BufFrames.ir(buffer)-1)
    //
    // NB  
    //   . Looping will occur regardless of the value of BufRd(loop:).ar.
    //   . Upon looping, Phasor index resets to zero.
    //
    if (synthPlaybackName.notNil, 
    {
      SynthDef( synthPlaybackName.asSymbol, 
        {  | 
             amp=1,  ampGate=0,
             outBus1=9999,  outBus2=9999,

             buffer,  bufRdInterpolation=2,
             startIndexInFrames=0,  endIndexInFrames=0,  
             rateAndDirection=1,  rateAndDirectionGate=1,

             queryPlaybackIndexGate=0,
             resetIndexInFrames=0,  resetIndexInFramesGate=0
           |
          var  phasor, bufrd, mono;

          var  numChannels  = this.soundfileBuffer.numChannels;


          //
          phasor = Phasor.ar( 
                     start:     startIndexInFrames,
                     end:       endIndexInFrames,

                     rate:      BufRateScale.ir(buffer) 
                                  * rateAndDirection * rateAndDirectionGate,

                     resetPos:  resetIndexInFrames,
                     trig:      resetIndexInFramesGate
                   );

          bufrd = BufRd.ar(   bufnum:         buffer, 
                              numChannels:    numChannels, 

                              phase:          phasor,
                              interpolation:  bufRdInterpolation,

                              loop:           1
                          );

          mono = Mix(bufrd);


          //
          SendReply.kr( trig:     queryPlaybackIndexGate, 
                        cmdName:  OSC.oscPathSynth 
                                        +/+ synthPlaybackName 
                                        +/+ OSC.oscTokenBufferPlaybackIndex,
                        values:   phasor
                      );


          //
          OffsetOut.ar([outBus1, outBus2], mono * amp * ampGate);
        },

        rates: [ 0.010, 0.010 ]
      ).store;
    });


    //
    ^'';

  }  //createPlaybackSynth


  //------------------------ -o-
  startPlaybackSynth  
  {
    synthPlayback.bundleControls(
                    nil, nil,
                    \rateAndDirectionGate,  1,
                    \ampGate,               1
                  );

    ^this;
  }

  //------------------------ -o-
  stopPlaybackSynth  
  {
    synthPlayback.bundleControls(
                    nil, nil,
                    \ampGate,               0,
                    \rateAndDirectionGate,  0
                  );

    ^this;
  }


  //------------------------ -o-
  // ASSUME  args represent arguments to Synth.set.
  //
  sendPlaybackSynthControlsWithGateResets  { | ...args
                                             |
    var  smallDelayToSerializeGateResets  = 0.050;  


    //
    synthPlayback.bundleControls(nil, nil, *args);

    synthPlayback.bundleControls(   
                    smallDelayToSerializeGateResets, 
                    nil, 
                    \queryPlaybackIndexGate, 0,
                    \resetIndexInFramesGate, 0
                  );
  }


  //------------------------ -o-
  // NB  Calling getPlaybackIndex and setPlaybackIndex simultaneously
  //     may cause trouble.  
  //
  getPlaybackIndex  
  {                                                             //FORKS 
    var  mainTimeDifference  = -1,
         rval                = -1;


    //
    if (this.isReleased(),   { ^nil; });
    if (this.isReady().not,  { ^nil; });

    if (isRunning.not, {
      Log.warning(thisFunction, 
            "CANNOT QUERY cursor index when instance is not running."
          ).postln; 

      ^nil;
    });


    //
    playbackIndexInFramesRaw      = -1;      
    playbackIndexInFramesIsReady  = false;  


    // Request frame index.
    //
    // ASSUME  synthPlayback responds immediately and all lag comes from 
    //         network return response and message processing...
    //
    mainTimeDifference = Main.elapsedTime;

    this.sendPlaybackSynthControlsWithGateResets( \queryPlaybackIndexGate, 1 ); 


    {
      rval = 
        Routine.spinlock(    
                                    { playbackIndexInFramesRaw >= 0; },
          testFunctionString:      "{ playbackIndexInFramesRaw >= 0; }",

          testIterationWait:       playbackIndexQueryWaitIteration,
          testIterationWaitMax:    playbackIndexQueryWaitIterationMax,

          disallowExceptionError:  true,
          logContext:thisFunction
          //, verbose:true
        );

      // NB  Spinlock should NEVER fail.
      //
      if (playbackIndexInFramesRaw < 0,
      {
        Log.error(thisFunction, 
                 "[%]  Spinlock FAILED to get "
              ++ "playbackIndexInFramesRaw within % seconds.", 
                    soundfileBasename, rval.abs
            ).postln; 

      }, {
        // Estimate actual frame index at: (time of request) + (time to receive request)
        //
        mainTimeDifference = Main.elapsedTime - mainTimeDifference;

        this.setPlaybackIndex(
          indexInFrames:             playbackIndexInFramesRaw,
          indexOffsetInSeconds:      mainTimeDifference,
          doNotUpdatePlaybackSynth:  true
        );
      });

      '';
    }.fork;


    //
    ^this;
  }


  //------------------------ -o-
  // Set playback cursor to new index: <frames> + <offset in seconds>.
  //
  // NB  In order to set index based on time alone, 
  //     playbackIndexInFrames MUST EQUAL zero (0).  Using nil defaults 
  //     to playbackIndexInFrames.
  //
  // NB  Sets cursor index immediately.
  //     Does not check for outstanding getPlaybackIndex() requests.
  //
  setPlaybackIndex  { | indexInFrames,
                        indexOffsetInSeconds,
                        wrapAroundBuffer,
                        doNotUpdatePlaybackSynth
                      |
    var  newPlaybackIndexInFrames  = -1,
         offsetInFrames            = -1,
         framesMaximum             = soundfileBuffer.frames(),
         secondsMaximum            = soundfileBuffer.framesToSeconds();


    //
    if (this.isReleased(),   { ^nil; });
    if (this.isReady().not,  { ^nil; });

    if (indexInFrames.isNil && indexOffsetInSeconds.isNil,
    {
      Log.error(thisFunction, 
               "[%]  MUST DEFINE AT LEAST one of indexInFrames "
            ++ "and indexOffsetInSeconds.",
                  soundfileBasename
          ).postln; 
      
      ^nil;
    });


    //DEFAULTS.
    indexInFrames             = indexInFrames ?? this.playbackIndexInFrames;

    indexOffsetInSeconds      = indexOffsetInSeconds      ?? 0;
    wrapAroundBuffer          = wrapAroundBuffer          ?? true;
    doNotUpdatePlaybackSynth  = doNotUpdatePlaybackSynth  ?? false;


    if (Parse.isInstanceOfClass(thisFunction, 
                 indexInFrames, "indexInFrames", Number).not,
                                                        { ^nil; });
    if (Parse.isInstanceOfClass(thisFunction, 
                 indexOffsetInSeconds, "indexOffsetInSeconds", Number).not,
                                                        { ^nil; });
    if (Parse.isInstanceOfClass(thisFunction, 
                 wrapAroundBuffer, "wrapAroundBuffer", Boolean).not, 
                                                        { ^nil; });
    if (Parse.isInstanceOfClass(thisFunction, 
          doNotUpdatePlaybackSynth, "doNotUpdatePlaybackSynth", Boolean).not, 
                                                        { ^nil; });

    //
    offsetInFrames =
                soundfileBuffer.secondsToFrames(indexOffsetInSeconds.abs)
              * indexOffsetInSeconds.sign;

    newPlaybackIndexInFrames = (indexInFrames + offsetInFrames);

    if (wrapAroundBuffer, 
    {
      newPlaybackIndexInFrames = (newPlaybackIndexInFrames % framesMaximum);

    }, {
      if (newPlaybackIndexInFrames < 0, {
        newPlaybackIndexInFrames = 0;

        Log.warning(thisFunction, 
              "[%]  BOUNDING LOW VALUE of newPlaybackIndexInFrames to zero.",
                 soundfileBasename
            ).postln; 
        
      });

      if (newPlaybackIndexInFrames >= framesMaximum, {
        newPlaybackIndexInFrames = framesMaximum - 1;

        Log.warning(thisFunction, 
              "[%]  BOUNDING HIGH VALUE of newPlaybackIndexInFrames to "
                 ++ "maximum number of frames less one (%).",
                 soundfileBasename, (framesMaximum - 1)
            ).postln; 
      });
    });


    //
    this.playbackIndexInFrames         = newPlaybackIndexInFrames;
    this.playbackIndexInFramesIsReady  = true;

    if (doNotUpdatePlaybackSynth.not, 
    {
      this.sendPlaybackSynthControlsWithGateResets(
             \resetIndexInFrames,      newPlaybackIndexInFrames,
             \resetIndexInFramesGate,  1
           );
    });


    //
    ^this;

  }  //setPlaybackIndex()


  //------------------------ -o-
  setPlaybackIndexPerPlaybackStrategy  
  {
    switch (playbackStrategy,

      // Intuition: Move playback cursor forward a multiple of one second.
      //
      \irwin, {
        this.setPlaybackIndex( 
                          this.playbackIndexInFrames 
                        + (playbackStrategyIrwinMultiple 
                             * this.soundfileBuffer.sampleRate)
             );
      },


      //
      \playFromBeginning, {
        this.setPlaybackIndex(0);
      },

      //
      \resetToBeginning, {
        this.setPlaybackIndex(0);
        playbackStrategy = playbackStrategyPrevious;
      },


      // Nothing to do: 
      //   synthPlayback starts from the point where it previously stopped.
      //
      // HOWEVER -- Update current index during run() :: sleepFunction.() .
      //
      \playFromLastIndex, {
        /*EMPTY*/
      },


      //
      \playFromRandomIndex, {
        this.setPlaybackIndex(soundfileBuffer.frames().rand);
      },

      //
      \resetToRandomIndex, {
        this.setPlaybackIndex(soundfileBuffer.frames().rand);
        playbackStrategy = playbackStrategyPrevious;
      },


      //
      {
        Log.error(thisFunction, 
              "[%]  UNKNOWN playbackStrategy.  (%)", 
                 soundfileBasename, playbackStrategy
            ).postln; 
      }
    );


    //
    ^this;
  }  


  //------------------------ -o-
  eventTimeInMilliseconds  { | multiplier
                             |
    var  rval  = -1;

    //DEFAULTS.
    multiplier = multiplier ?? eventTimeMultiplier;

    if (Parse.isInstanceOfClass(thisFunction, 
                  multiplier, "multiplier", Number).not, { ^this; });

    if (multiplier < 0, 
    {
      Log.error(thisFunction, 
            "[%]  multiplier MUST BE greater than zero.  (%)", 
               soundfileBasename, multiplier
          ).postln; 

      ^this; 
    });


    //
    eventTimeInMillisecondsShadow = 
                 (this.class.millisecondDurationsArray.choose * multiplier);

    ^eventTimeInMillisecondsShadow;
  }




  //---------------------------------------------------------- -o--
  // Demo.

  *demo                                                 //FORKS
  { 
    var  platformSoundsDirectory  = Platform.resourceDir +/+ "sounds";
    
    var  filesInWorkingDirectory         = nil,
         filesInPlatformSoundsDirectory  = nil,
         allSoundFiles;

    var  useOnlyCustomSamples  = false;

    var  countOfLongSampleInstances  = 15,
         oneInstance,
         minimumMemSize              = (1024 * 1024) * 10,  //megabytes
         rval                        = nil;

    var  oscMutedPreviousValue  = OSC.messageCountMuted,
         logContext             = thisFunction;


    //
    Log.info(logContext, "BEGIN").postln; 

    //useOnlyCustomSamples  = true;   //CONFIG

                                // Memory requirements.
                                //
    if (Server.default.options.memSize < minimumMemSize, 
    {
      {
        Server.default.quit;

        Server.default.options.memSize = minimumMemSize;
        Server.default.boot;

        Server.default.waitForBoot();

      }.fork;
    });

    OSC.messageCountMuted = true;
    Z.dashboardSystem();


                                // Usage.
                                // Singleton structure.
                                //
    "".postln;
    LongSample.usage().postln;
    "".postln;
    LongSample.pretty().postln;


    //
    filesInWorkingDirectory = 
      File.findFilesUnderArrayOfDirectoriesWithSuffixPattern(
         [LongSample.workingDirectory], [\aif, \wav],
         returnFullPath:true
      );

    if (useOnlyCustomSamples.not, 
    {
      filesInPlatformSoundsDirectory = 
        File.findFilesUnderArrayOfDirectoriesWithSuffixPattern(
          [platformSoundsDirectory], [\aif, \wav],
          returnFullPath:true
        );

      filesInPlatformSoundsDirectory.do({ | elem, i
                                          |  
          if (elem.regexpMatch("/a11wlk01").not, { 
            filesInPlatformSoundsDirectory.removeAt(i); 
          }); 
        }); 
    });


    allSoundFiles = 
      filesInWorkingDirectory ++ filesInPlatformSoundsDirectory;

    "".postln;
    Log.info(logContext, "Available sound files...").postln; 
    allSoundFiles.pretty(elisionLength:1024).postln;

                 
    //
    "".postln;
    {
      rval = Routine.spinlock(
                {    Server.default.serverRunning 
                  && Server.default.serverBooting.not; },
                disallowExceptionError:true
                //, verbose:true
              );

      if (rval < 0, {
        Log.error(thisFunction,
                 "FAILED to boot Server with necessary memory requirements"
              ++ " (%).  Aborting %.demo()...",
                    minimumMemSize.sizeInBytes, this.classname
            ).postln; 

      }, {
                                // Initialize.  Once per Server session.
                                //   Currently this means creating group 
                                //     heirarchy for LongSample.
                                //
                                //   Waiting means group namespace will 
                                //     contain all LongSample activity.
                                //   This is OPTIONAL, but skipping it
                                //     creates a mess of unused groups...
                                //
        LongSample.init();   
        0.1.wait;

                                // Demonstrate group namespace heirarchy.
                                //
        MobileSound.pretty(\group).postln;
        Suites.pretty(\group).postln;
        Schumacher.pretty(\group).postln;


                                // Generate a bunch of instances.
                                //
                                // Wait for them all to finish loading.
                                //   Waiting is OPTIONAL, but doing so
                                //     means BufferCache will consistently
                                //     register all Buffer.load() activity.
                                //
                                // Dump the list of sample Buffers.
                                // Dump the list of instances.
                                //
        countOfLongSampleInstances.do({ 
          LongSample(allSoundFiles.choose);
          0.25.wait;   
        });

        while ( { LongSample.isReadyAll().not; }, { 1.wait; });

        "".postln;
        BufferCache.pretty().postln;

        "".postln;
        LongSample.pretty(\playbackSynthInstanceDictionary).postln;
        2.wait;


                                // Play everything for a bit.
                                // Then stop everything.
                                //
        "".postln;
        Log.info(logContext, "LongSample instances START...").postln; 
        LongSample.runStopAll();
        10.wait;

        "".postln;
        Log.info(logContext, "LongSample instances STOP...").postln; 
        LongSample.runStopAll();
        5.wait;


                                // Select one instance.
                                // Manually place its output on 
                                //   channels [0,1].
                                // Demonstrate iteration.
                                //
        "".postln;
        Log.info(logContext, "ITERATE with one instance...").postln; 

        oneInstance = 
             LongSample.instanceAt(LongSample.psid.keys.choose.asInteger);

        oneInstance.outputBusOrBusses
                      .isManualBecomesAutomaticallyActive = true;
        oneInstance.channel2DelayTimeInSeconds
                      .isManualBecomesAutomaticallyActive = true;

        //
        oneInstance.runStop();

        5.do({
          oneInstance.outputBusOrBusses = [0, 1].scramble;
          oneInstance
            .channel2DelayTimeInSeconds = rrand(1.0, 4.0).round(0.001);

          oneInstance.iterate().pretty().postln;
          7.wait;
        });

        oneInstance.runStop();
        oneInstance.manualOff();
        oneInstance.pretty().postln;
        5.wait;


                                // Return to ensemble play for a while...
                                //
        "".postln;
        Log.info(logContext, "LongSample instances START...").postln; 
        LongSample.runStopAll();
        60.wait;

        "".postln;
        Log.info(logContext, "LongSample instances STOP...").postln; 
        LongSample.runStopAll();
        5.wait;


                                // Cleanup all instances.
                                // Demonstrate all instances have been
                                //   been cleaned up.
                                //
        "".postln;
        Log.info(logContext, "LongSample instances RELEASE...").postln; 
        LongSample.releaseAll();
        1.wait;

        "".postln;
        LongSample.pretty(\playbackSynthInstanceDictionary).postln;

        "".postln;
        BufferCache.pretty().postln;


        //
        "".postln;
        Log.info(logContext, "END").postln; 

        OSC.messageCountMuted = oscMutedPreviousValue;

      });  //endif -- Routine.spinlock
    }.fork;

    '';
  }




  //---------------------------------------------------------- -o--
  // MOS protocol methods.

  //------------------------ -o-
  pretty  { | pattern, elisionLength, depth,
              casei, compileString, indent, initialCallee, 
              enumerate, bulletIndex, enumerationOffset,
              minimumSpacerSize, nolog, shortVersion
            |
    var  title  = this.classname.toUpper,
         ampMasterLabel  = "ampMaster";

    var  maxKeyLength  = "channel1BandpassFilterFrequencyPair (IP)".size;

    var  enviroA1, enviroA2, enviroA3, enviroA4, enviroA5, enviroA6, enviroA7,
         enviroB1, enviroB2, 
         enviroC1, enviroC2, enviroC3, enviroC4,
         enviroD1, enviroD2, 
         enviroE1, enviroE2,
         enviroF1, enviroF2,
         enviroG1, enviroG2,
         enviroArray;

    var  soundfileBufferSecondsString  = Object.labelUndefined;


    //DEFAULTS.
    shortVersion = shortVersion ?? true;

    title = ("%  %").format(title, soundfileBasename);


    //
    enviroA1 = (
      playbackStrategy:         playbackStrategy,
    );

    enviroA3 = (
      isRunning:                isRunning,
    );

    enviroA4 = (
      verbose:                  verbose,
    );

                //SPACER

    enviroA5 = (
      eventTimeInMilliseconds:  
                   ("% (%)").format(
                      eventTimeInMillisecondsShadow,
                      eventTimeInMillisecondsShadowLabel
                   ),

      eventTimeMultiplier:      eventTimeMultiplier,
      eventTimeMultiplierSleepRatio:
                                eventTimeMultiplierSleepRatio,
    );


    if (soundfileBuffer.notNil && soundfileBuffer.isUndefined().not, 
    { 
      soundfileBufferSecondsString = 
                              soundfileBuffer
                                .framesToSeconds(this.playbackIndexInFrames)
                                .roundPadRight(0.001);
    });

    enviroA6 = (
      playbackIndexInFrames: 
                ("% (%s)").format( 
                             this.playbackIndexInFrames.round(0.1),
                             soundfileBufferSecondsString
                           ),

      playbackStrategyIrwinMultiple: 
                                playbackStrategyIrwinMultiple,
    );

                //SPACER

    enviroA7 = (
      playbackIndexQueryWaitIteration: 
                                playbackIndexQueryWaitIteration
                                  .roundPadRight(0.001),
      playbackIndexQueryWaitIterationMax: 
                                playbackIndexQueryWaitIterationMax
                                  .roundPadRight(0.001),
    );

                //SPACER

    if (ampMasterShadow.isBus, {
      ampMasterLabel = ampMasterLabel ++ " (BUS)";
    });

    enviroC1 = (
      ampMasterLabel.():        this.ampMaster,
    );

    enviroC2 = (
      ampChannels:              ampChannels.pr(depth:0),
    );

    enviroC3 = (
      "outputBusOrBusses (IP)": outputBusOrBusses.pr,
    );

    enviroC4 = (
      "channel1BandpassFilterFrequencyPair (IP)":  
                                channel1BandpassFilterFrequencyPair.pr,
      "channel2BandpassFilterFrequencyPair (IP)":  
                                channel2BandpassFilterFrequencyPair.pr,
      "channel2DelayTimeInSeconds (IP)":  
                                channel2DelayTimeInSeconds.pr,
    );

                //SPACER

    enviroD1 = (
      allowSetParameterBetweenIterations:  allowSetParameterBetweenIterations,
      isManualBecomesAutomaticallyActive:  isManualBecomesAutomaticallyActive, 
    );

    enviroD2 = (
      server:                   server.name,
    );

                //SPACER

    enviroB1 = (
      soundfilePathname:        soundfilePathname,
    );

    enviroB2 = (
      soundfileBuffer:          soundfileBuffer.pretty(nolog:true).minel(elisionLength:128),

      soundfileBufferLoadWaitIterationMax: 
                                soundfileBufferLoadWaitIterationMax
                                  .roundPadRight(0.001),
    );

                //SPACER

    enviroE1 = (
      group:                    this.group.pretty(nolog:true).minel(elisionLength:128),
      synthPlayback:            synthPlayback.pretty(nolog:true).minel(elisionLength:128),
    );

    enviroE2 = (
      audioBus1:                audioBus1,
      audioBus2:                audioBus2,
    );

                //SPACER

    enviroF1 = (
      group1Channel:         group1Channel.pretty(nolog:true).minel(elisionLength:128),
    );

    enviroF2 = (
      synth1BandpassFilter:  synth1BandpassFilter.pretty(nolog:true).minel(elisionLength:128),
      synth1Route:           synth1Route.pretty(nolog:true).minel(elisionLength:128),
    );

                //SPACER

    enviroG1 = (
      group2Channel:         group2Channel.pretty(nolog:true).minel(elisionLength:128),
    );

    enviroG2 = (
      synth2BandpassFilter:  synth2BandpassFilter.pretty(nolog:true).minel(elisionLength:128),
      synth2Delay:           synth2Delay.pretty(nolog:true).minel(elisionLength:128),
      synth2Route:           synth2Route.pretty(nolog:true).minel(elisionLength:128),
    );


    //
    if (shortVersion, {
      enviroArray = [ enviroA1, enviroA3, (), enviroA5, enviroA6, (),
                      enviroC1, enviroC2, enviroC3, enviroC4, (),
                      enviroD1, enviroD2,
                    ];
    }, {
      enviroArray = [ enviroA1, enviroA3, enviroA4, (), enviroA5, enviroA6, (), enviroA7, (),
                      enviroC1, enviroC2, enviroC3, enviroC4, (),
                      enviroD1, enviroD2, (),
                      enviroB1, enviroB2, (),
                      enviroE1, enviroE2, (),
                      enviroF1, enviroF2, (),
                      enviroG1, enviroG2,
                    ];
    });

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
    var  title            = this.classname.toUpper,
         psiDictOrString  = "";

    var  maxKeyLength  = "".size;

    var  enviroA, enviroB, enviroC, enviroD, enviroE,
         enviroArray;  // ADD


    var  delayTimesString = delayTimesInSecondsArray.asString;


    //
    enviroD = (
      workingDirectory:               workingDirectory,
    );

                //SPACER

    enviroC = (
      playbackStrategies:               playbackStrategies.asString,
    );

                //SPACER

    enviroA = (
      delayTimesInSecondsArray:         delayTimesInSecondsArray.asString,
      //delayTimesInSecondsArray:         delayTimesInSecondsArray.pr(depth:0),

      millisecondDurationsArray:        millisecondDurationsArray.asString,
    );

                //SPACER

    enviroB = (
      bandpassFilterFrequencyPairsArray: 
                                        bandpassFilterFrequencyPairsArray,
    );

                //SPACER

    psiDictOrString = if (playbackSynthInstanceDictionary.size > 0, { 
      playbackSynthInstanceDictionary; 
    }, { 
      Object.labelEmpty;
    });

    enviroE = (
      playbackSynthInstanceDictionary:  psiDictOrString,
    );


    //
    enviroArray = [ enviroD, (), 
                    enviroC, (), 
                    enviroA, (), 
                    enviroB, (),
                    enviroE,
                  ];
                        // ADD

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


}  //LongSample

