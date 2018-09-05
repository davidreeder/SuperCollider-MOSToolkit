//
// Node_MOS.sc
//
//
// PUBLIC METHODS--
//   *newRegistered
//   *isPlaying  *isRunning 
//
//   pauseContinue
//   sampleRate
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//   pretty
//
//
// MOS DEPENDENCIES--
//   Object_MOS
//   Parse
//   String_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Node  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Class methods.

  //----------------- -o-
  // NB  Overridden by Group.newRegistered to simplify 
  //     Synth-centric argument list.
  //
  *newRegistered  { | defName, args, target, addAction,
                      server,
                      paused
                    |
    var  node  = nil;

    //DEFAULTS.
    server     = server     ?? Server.default;
    target     = target     ?? server.defaultGroup;
    addAction  = addAction  ?? \addToHead;
    paused     = paused     ?? false;


    //
    if (server.serverBooting, {
      Log.error( thisFunction,
                 "CANNOT CREATE Node: server % is STILL BOOTING...",
                    server.name.asString.quote
               ).postln;
      ^nil;
    });


    //
    server.makeBundle(nil, 
    {
      if (Synth == this, 
      {
        var  defNameSymbol  = defName.asSymbol;

        //
        if (Parse.isInstanceOfClass(
                    thisFunction, defName, "defName", Symbol).not, 
                                                        { ^nil; });
        //
        if (paused, {
          node = Synth.newPaused(defName, args, target, addAction);
        }, {
          node = Synth.new(defName, args, target, addAction);
        });

      }, {
        node = Group.new(target, addAction);
        node.run(paused.not);
      });

      NodeWatcher.register(node);
    });

    ^node;
  }


  //----------------- -o-
  *isPlaying  { |node|                  // Node is active on server.
    if (node.notNil, { ^node.isPlaying; });
    ^false;
  }

  *isRunning  { |node|                 // Node is running on server.
    if (node.notNil, { ^node.isRunning; });
    ^false;
  }




  //------------------------------------------- -o--
  // Instance methods.

  //----------------- -o-
  pauseContinue  { | runValue
                   |
    var  boolValue  = runValue ?? this.isRunning.not;

    if (this.isPlaying.not, {
      Log.error( thisFunction, 
                 "%.isPlaying returns FALSE.", this.asString
               ).postln; 
      ^this;
    });

    //
    this.run(boolValue);

    ^this;
  }


  //----------------- -o-
  sampleRate  { ^this.server.sampleRate; }




  //------------------------------------------- -o--
  // MOS Protocol Methods.

  //----------------- -o-
  pretty  { | pattern, elisionLength, depth,
              casei, compileString, indent, initialCallee, 
              enumerate, bulletIndex, enumerationOffset,
              minimumSpacerSize, nolog, shortVersion
            |
    var  title    = this.classname.toUpper,
         playrun  = "";

    var  maxKeyLength  = "sampleRate".size;

    var  enviroA1, enviroA2, enviroA3, enviroA4,
         enviroArray;


    //
    if (Synth == this.class, {
      title = ("%  %").format(title, this.defName);
    });


    //
    enviroA1 = (
      id:               nodeID,
    );

    enviroA2 = (
      gid:              if (group.isNil, { "nil"; }, { group.nodeID; }),
    );

    enviroA3 = (
      server:           this.server.name,
      sampleRate:       this.sampleRate,
    );

    //NB  .isPlaying/.isRunning are only valid for Nodes registered
    //    with NodeWatcher.  See .newRegistered().
    //
    enviroA4 = (
      playrun:          ("%/%").format(this.isPlaying, this.isRunning),
    );


    //
    enviroArray = [ enviroA1, enviroA2, enviroA3, enviroA4,
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


} //Node

