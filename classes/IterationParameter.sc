//
// IterationParameter.sc
//
// This helper class represents the value of a parameter held by a 
// referring class instance (referringInstance).  The referring 
// instance sets the value of IterationParameter and instructs it when
// to iterate.  IterationParameter also contains functions, written in 
// the context of referringInstance, that define how its value is to be 
// applied and how to self-generate new values.
// 
// The intuition--
//   . In general, IterationParameter variables are not set directly, but
//     instead rely on new values generated at the time of their iteration.
//     Iteration is managed by valueResetFunction via iterateFunction.
// 
//   . When an IterationParameter is set, the new value placed in
//     waiting, to be taken at the next iteration instead of the internal
//     value generating function (valueResetFunction), UNLESS the
//     IterationParameter is overriden to set the current value 
//     immediately.  Override occurs if allowSetParameterBetweenIterations 
//     is set in the IterationParameter or in the referringInstance.  
//     Changing the current value of IterationParameter will immediately 
//     affect anything that relies upon it.
// 
//   . Setting an IterationParameter may also freeze the current value,
//     putting it into a "manual" state, so that iterations no longer 
//     activate the value generating function.  Manual state can be set 
//     directly, or at the time of iteration to a previously set new value 
//     if isManualBecomesAutomaticallyActive is set in the 
//     IterationParameter or in the referringInstance.
// 
// 
// IterationParameter should never retain resources of its own, though
// it likely refers to resources in referringInstance.
// 
// IterationParameter REQUIRES the following public variables, of Boolean
// type, to be exposed via referringInstance:
// 
//    isManualBecomesAutomaticallyActive
//    allowSetParameterBetweenIterations
//
// Local instances of these variables override those in the 
// referringInstance.
// 
// iterateFunctionString and valueResetFunctionString MAY REQUIRE other 
// public variables or methods exposed via referringInstance.  
// FunctionStrings MUST evaluate to a Function via compile().  By taking 
// the following form, the Functions are given access to the 
// IterationParameter instance and referringInstance:
//
//   "{ |self, referringInstance|  ...; }"
//         -OR-
//   { |self, referringInstance|  ...; }.cs
// 
// parseValueFunction has the signature: |self, referringInstance, value|
// it SETS newValue and RETURNS true on success, false on error.
//
//
// Setting IterationParameter entails setting either value or nextValue, 
// per the following logic:
// 
//   . value and nextValue MUST both be of the same type, though
//     either or both may be nil.
// 
//   . Changing value executes iterateFunctionString(), effecting 
//     changes in referringInstance.  
// 
//   . Setting (value = nil) automatically executes 
//     valueResetFunctionString() which sets value to a non-nil value.
// 
//   . If allowSetParameterBetweenIterations is true, setting
//     IterationParameter will update value immediately and nextValue
//     is reset to nil.  Otherwise, nextValue is updated.
// 
//   . iterate() may also update value.  If (nextValue != nil), then
//     (value = nextValue) and nextValue is reset to nil.  However, if
//     nextValue is already nil, and isManual is false, then value is
//     updated per valueResetFunctionString().  isManual prevents value 
//     from being set in the absense of IterationParameter being 
//     explicitly set.
// 
//   . isManual may be set or cleared at any time.
// 
//   . If isManualBecomesAutomaticallyActive is true, then (isManual =
//     true) automatically whenever nextValue is consumed by value.
//     Otherwise, iterate() will change value even if nextValue is nil.
// 
//
// PUBLIC CLASS METHODS--
//   *new  
//   *usage
//   *demo
//
//   *setManualAll
//     *setManualAllOn  *setManualAllOff
//
//   *iterateAll  *iterateNowAll
//
// PUBLIC METHODS--
//   set  
//   setNow  [iterateNow]
//   get  
//   valueReset
//   iterate
//
// PRIVATE METHODS--
//   assertExistenceOfReferringInstanceElements  
//     assertExistenceOfReferringInstanceVariables  
//     assertExistenceOfReferringInstanceMethods  
//
//
// MOS PROTOCOL SUPPORT--
//   (*)pretty
//   prretty
//
//   (*)mosVersion
//
// SUPPORT METHODS--
//   initIterationParameter
//   printOn  storeOn  storeArgs
//
//
// DEPENDENCIES--
//   Log 
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


IterationParameter : MobileSound
{
  classvar  classVersion = "0.1";   //RELEASE

  classvar  invocationArgs;


  // Public variables.
  //
  var  <  value,
       <  nextValue,
       <  valueClassType,
       <  isManual;

  var  <  parameterLogContext,
       <  parameterName,
       <  referringInstance,
       <  referringVariables,
       <  referringMethods;

  var  <  parseValueFunction,
       <  valueResetFunction,
       <  iterateFunction;

  var  <  allowSetParameterBetweenIterations,
       <  isManualBecomesAutomaticallyActive;

  var     verbose;


  // Private variables.
  //
  var  <> newValue;


  //---------------------------------------------------------- -o--
  *mosVersion { ^super.mosVersion(classVersion); }
  mosVersion  { ^this.class.mosVersion; }




  //---------------------------------------------------------- -o--
  // Constructors, state.

  //------------------------ -o-
  *initClass  {
    invocationArgs = "parameterLogContext, valueClassType, referringInstance, parseValueFunctionString, valueResetFunctionString, iterateFunctionString, [isManual, referringVariables, referringMethods]"; 
  }


  //------------------------ -o-
  *new  { | parameterLogContext,
            valueClassType,
            referringInstance,
            parseValueFunctionString,
            valueResetFunctionString,
            iterateFunctionString,
            isManual,
            referringVariables,
            referringMethods
          |
    ^super.new.initIterationParameter( parameterLogContext,
                                       valueClassType,
                                       referringInstance,
                                       parseValueFunctionString,
                                       valueResetFunctionString,
                                       iterateFunctionString,
                                       isManual,
                                       referringVariables,
                                       referringMethods
                                     );
  }


  //------------------------ -o-
  // DEFAULTS.
  //
  initIterationParameter  { | parameterLogContext,
                              valueClassType,
                              referringInstance,
                              parseValueFunctionString,
                              valueResetFunctionString,
                              iterateFunctionString,
                              isManual,
                              referringVariables,
                              referringMethods
                            |
    var  rval  = false;

    //DEFAULTS.
    verbose = true;
    allowSetParameterBetweenIterations = nil;
    isManualBecomesAutomaticallyActive = nil;

    //
    this.parameterLogContext       = parameterLogContext;
    this.parameterName             = parameterLogContext.methodname;
    this.valueClassType            = valueClassType;
    this.referringInstance         = referringInstance;
    this.parseValueFunctionString  = parseValueFunctionString;
    this.valueResetFunctionString  = valueResetFunctionString;
    this.iterateFunctionString     = iterateFunctionString;
    this.isManual                  = isManual ?? false;
    this.referringVariables        = referringVariables;
    this.referringMethods          = referringMethods;


    rval = this.assertExistenceOfReferringInstanceVariables(
                              [
                                \isManualBecomesAutomaticallyActive,
                                \allowSetParameterBetweenIterations
                              ]);
    this.valueReset();



    //
    if (    this.parameterLogContext.isNil
         || this.valueClassType.isNil
         || this.referringInstance.isNil 
         || this.parseValueFunction.isNil
         || this.valueResetFunction.isNil
         || this.iterateFunction.isNil
         || this.isManual.isNil
         || (referringVariables.notNil && this.referringVariables.isNil)
         || (referringMethods.notNil && this.referringMethods.isNil)
         || rval.not
         || this.value.isNil
          ,
    {
      Log.error( thisFunction, 
                 "Initialization FAILED (for %).", parameterName
               ).postln;
      ^nil;
    });

    ^this;
  }


  //------------------------ -o-
  instanceArgs  { | stringOrArray
                  |
    var  errorMessage  = 
                ("stringOrArray MUST be \string or \array (for %)")
                    .format(parameterName);

    //
    if (Parse.isInstanceOfClass(thisFunction, 
                stringOrArray, thisFunction.methodname, Symbol).not, {
      ^Log.error(thisFunction, errorMessage);
    });

    //
    switch (stringOrArray, 
      //
      \string, 
         { ^("%, %, %, %, %, %, %, %, %")
               .format( 
                  parameterLogContext,    
                  valueClassType,
                  referringInstance.cs,
                  parseValueFunction.cs,
                  valueResetFunction.cs,
                  iterateFunction.cs,
                  isManual,
                  referringVariables.cs,
                  referringMethods
               );
         },

      //
      \array,  
         {
           ^[ 
                  parameterLogContext,   
                  valueClassType,
                  referringInstance,
                  parseValueFunction,
                  valueResetFunction,
                  iterateFunction,
                  isManual,
                  referringVariables,
                  referringMethods
            ];
         },

      //
      { ^Log.error(thisFunction, errorMessage); }
    );

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
  printOn   { |stream|  stream << ("%").format(this.value); }  //NB

  //
  storeOn  { |stream|                           
    stream << ("%.new(%)").format(this.classname, this.instanceArgs(\string));
  }

  //
  storeArgs  { |stream|  ^this.instanceArgs(\array).cs; }




  //---------------------------------------------------------- -o--
  // Getters/setters.

  //------------------------ -o-
  parameterName_  { | value
                    |
    case
      { Parse.isInstanceOfClass(thisFunction, 
              value, thisFunction.methodname, String).not; 
      }
          { /*EMPTY*/ }

      { parameterName = value; }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  parameterLogContext_  { | value
                          |
    case
      { Parse.isInstanceOfClass(thisFunction, 
              value, thisFunction.methodname, Function).not; 
      }
          { /*EMPTY*/ }

      { parameterLogContext = value; }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  valueClassType_  { | value
                     |
    case
      { Parse.isInstanceOfClass(thisFunction, 
                value, thisFunction.methodname, Class).not; }
          { /*EMPTY*/ }

      { valueClassType = value; }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  referringInstance_  { | value
                        |
    case 
      { value.isInstance.not; }
          { Log.error( thisFunction, 
                       "% MUST be an instance of some class (for %).",
                          thisFunction.methodname, parameterName
                     ).postln; 
          }

      { value.isNil; }
          { Log.error( thisFunction, 
                       "% CANNOT be nil (for %).", 
                          thisFunction.methodname, parameterName
                     ).postln; 
          }

      { referringInstance = value; }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  parseValueFunctionString_  { | value
                               |
    case
      { Parse.isInstanceOfClass(thisFunction, 
                value, thisFunction.methodname, String).not; 
      }
          { /*EMPTY*/ }

      { parseValueFunction = value.compile();
        Parse.isInstanceOfClass(thisFunction,
                parseValueFunction, thisFunction.methodname, Function).not; 
      }
          { parseValueFunction = nil; }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  valueResetFunctionString_  { | value
                               |
    case
      { Parse.isInstanceOfClass(thisFunction, 
                value, thisFunction.methodname, String).not; 
      }
          { /*EMPTY*/ }

      { valueResetFunction = value.compile();
        Parse.isInstanceOfClass(thisFunction,
                valueResetFunction, thisFunction.methodname, Function).not; 
      }
          { valueResetFunction = nil; }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  iterateFunctionString_  { | value
                            |
    case
      { Parse.isInstanceOfClass(thisFunction, 
                value, thisFunction.methodname, String).not; 
      }
          { /*EMPTY*/ }

      { iterateFunction = value.compile();
        Parse.isInstanceOfClass(thisFunction, 
                iterateFunction, thisFunction.methodname, Function).not; 
      }
          { iterateFunction = nil; }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  isManual_  { | value
               |
    case
      { Parse.isInstanceOfClass(thisFunction, 
                value, thisFunction.methodname, Boolean).not; }
          { /*EMPTY*/ }

      { isManual = value; }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  referringVariables_  { | value
                         |
    case
      { Parse.isInstanceOfClass(thisFunction, 
                value, thisFunction.methodname, Array, isOkayToBeNil:true).not; 
      }
          { /*EMPTY*/ }

      { if (value.isNil, {
          false;
        }, {
          this.assertExistenceOfReferringInstanceVariables(value).not;
        });
      }
          { /*EMPTY*/ }

      { referringVariables = value; }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  referringMethods_  { | value
                       |
    case
      { Parse.isInstanceOfClass(thisFunction, 
                value, thisFunction.methodname, Array, isOkayToBeNil:true).not; 
      }
          { /*EMPTY*/ }

      { if (value.isNil, {
          false;
        }, {
          this.assertExistenceOfReferringInstanceMethods(value).not; 
        });
      }
          { /*EMPTY*/ }

      { referringMethods = value; }

    ; //endcase

    ^this;
  }



  //------------------------ -o-
  allowSetParameterBetweenIterations_  { | value
                                         |
    case
      { Parse.isInstanceOfClass(thisFunction, value,
              thisFunction.methodname, Boolean, isOkayToBeNil:true).not; }
          { /*EMPTY*/ }

      //SUCCESS.
      { allowSetParameterBetweenIterations = value; }

    ; //endcase

    ^this;
  }


  //------------------------ -o-
  isManualBecomesAutomaticallyActive_  { | value
                                         |
    case
      { Parse.isInstanceOfClass(thisFunction, value,
              thisFunction.methodname, Boolean, isOkayToBeNil:true).not; }
          { /*EMPTY*/ }

      //SUCCESS.
      { isManualBecomesAutomaticallyActive = value; }

    ; //endcase

    ^this;
  }




  //---------------------------------------------------------- -o--
  // Public class methods.

  //------------------------ -o-
  *setManualAll  { | arrayOfIterationParameterInstances, 
                     booleanValue
                   |
    //
    if (Parse.isInstanceOfClass(thisFunction,
                arrayOfIterationParameterInstances,
                "arrayOfIterationParameterInstances", Array).not, { ^nil; });

    if (Parse.isInstanceOfClass(thisFunction, 
                booleanValue, "booleanValue", Boolean).not, { ^nil; });


    //
    arrayOfIterationParameterInstances.do({ |elem|
      elem.isManual = booleanValue;
    });

    ^arrayOfIterationParameterInstances;
  }


  //------------------------ -o-
  *setManualAllOn  { | arrayOfIterationParameterInstances
                     |
    ^this.setManualAll(arrayOfIterationParameterInstances, true);
  }


  //------------------------ -o-
  *setManualAllOff  { | arrayOfIterationParameterInstances
                      |
    ^this.setManualAll(arrayOfIterationParameterInstances, false);
  }


  //------------------------ -o-
  *iterateAll  { | arrayOfIterationParameterInstances,
                   iterateNow
                 |
    //DEFAULTS.
    iterateNow = iterateNow ?? false;

    if (Parse.isInstanceOfClass(thisFunction,
                arrayOfIterationParameterInstances,
                "arrayOfIterationParameterInstances", Array).not,  { ^nil; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  iterateNow, "iterateNow", Boolean).not,          { ^nil; });

    //
    arrayOfIterationParameterInstances.do({ | elem
                                            |
      if (iterateNow, {
        elem.iterateNow();
      }, {
        elem.iterate();
      });
    });

    ^arrayOfIterationParameterInstances;
  }


  // Overrides manual settings for one iteration.
  //
  *iterateNowAll  { | arrayOfIterationParameterInstances
                    |
     ^this.iterateAll(arrayOfIterationParameterInstances, true);
   }




  //---------------------------------------------------------- -o--
  // Public instance methods.

  //------------------------ -o-
  get  { ^this.value; }


  //------------------------ -o-
  set  { | incomingValue,
           iterateNow
         |
    var  allowSetParameterBetweenIterationsLocal,
         rval  = false;


    //
    if (Parse.isInstanceOfClass(thisFunction, 
              iterateNow, "iterateNow", Boolean, isOkayToBeNil:true).not, { 
      ^this; 
    });

    //DEFAULTS.
    iterateNow = iterateNow ?? false;

    allowSetParameterBetweenIterationsLocal = 
              allowSetParameterBetweenIterations
                ?? referringInstance.allowSetParameterBetweenIterations;


    //
    newValue = nil;

    rval = parseValueFunction.().(this, referringInstance, incomingValue);

    if (rval.not, {
      if (verbose, {
        Log.error( thisFunction, 
                   "% value is INVALID.  (%)", 
                      parameterName, incomingValue.prettyShort
                 ).postln; 
      });

      ^this;
    });


    //
    if (iterateNow || allowSetParameterBetweenIterationsLocal, {
      value = newValue;
      iterateFunction.().(this, referringInstance);

    }, {
      nextValue = newValue;
    });


    //
    ^this;
  }

  // Overides manual setting for one iteration.
  //
  setNow      { |incomingValue|  ^this.set(incomingValue, true); }
  iterateNow  { |incomingValue|  ^this.set(incomingValue, true); }   //ALIAS



  //------------------------ -o-
  valueReset  
  {
    var  localValue,
         rval;

    //
    if (Parse.isInstanceOfClass(thisFunction, 
          valueResetFunction, "valueResetFunction", Function).not, { ^this; });
         
    if (Parse.isInstanceOfClass(thisFunction, 
          parseValueFunction, "parseValueFunction", Function).not, { ^this; });


    //
    newValue    = nil;
    localValue  = valueResetFunction.().(this, referringInstance);
    rval        = parseValueFunction.().(this, referringInstance, localValue);

    if (rval.not, {
      if (verbose, {
        Log.error( thisFunction, 
                   "% value is INVALID.  (%)", 
                      parameterName, localValue.prettyShort
                 ).postln; 
      });

      ^this;
    });


    //
    value = newValue;

    ^this;
  }


  //------------------------ -o-
  // XXXNB  Variables to referringInstance are listed as variables 
  //        and methods of this.  Driven (at least) by instance of
  //        referringInstance below.
  //
  iterate  
  { 
    var  isManualBecomesAutomaticallyActiveLocal,
         hasValueChanged  = false;  


    //DEFAULTS.
    isManualBecomesAutomaticallyActiveLocal = 
              isManualBecomesAutomaticallyActive
                ??  referringInstance.isManualBecomesAutomaticallyActive;


    //
    if (nextValue.notNil, {
      value = nextValue;
      nextValue = nil;

      hasValueChanged = true;

      if (isManualBecomesAutomaticallyActiveLocal, {
        isManual = true;
      });

    }, {
      if (isManual.not, {
        this.valueReset();
        hasValueChanged = true;
      });
    });


    //
    if (hasValueChanged, {
      iterateFunction.().(this, referringInstance);
    });

    ^this.value;
  }




  //---------------------------------------------------------- -o--
  // Private instance methods.

  //------------------------ -o-
  assertExistenceOfReferringInstanceElements  { | arrayOfSymbols,
                                                  checkAsVariables
                                                |
    var  elementType  = "variable",
         allAreTrue   = true,
         rval;

    //
    if (Parse.isInstanceOfClass(thisFunction, 
              arrayOfSymbols, "arrayOfSymbols", Array).not, { ^false; });

    //DEFAULTS.
    checkAsVariables = checkAsVariables ?? true;

    if (checkAsVariables.not, { elementType = "method"; });


    // Test for existence.
    //
    arrayOfSymbols.do({ |elem|
      if (checkAsVariables, {
        rval = referringInstance.hasInstanceVariable(elem.asSymbol);
      }, {
        rval = referringInstance.hasInstanceMethod(elem.asSymbol);
      });

      if (rval.not, {
        allAreTrue = false;

        Log.error( thisFunction, 
                   "[%]  Instance object % DOES NOT PRESENT % \"%\".", 
                      parameterName, referringInstance, elementType, elem
                 ).postln; 
      });
    });


    // Test for definition.
    //
    if (checkAsVariables, {
      arrayOfSymbols.do({ |elem|
        if (referringInstance.hasInstanceVariable(elem.asSymbol), 
        {
          rval = ("{ |refInst|  refInst.%.isNil; }")
                        .format(elem.asSymbol.asString)
                           .compile.().(referringInstance);

          if (rval, {
            allAreTrue = false;

            Log.error( thisFunction, 
                       "[%]  Instance object % DOES NOT DEFINE % \"%\".", 
                          parameterName, referringInstance, elementType, elem
                     ).postln; 
          });
        });
      });
    });

    ^allAreTrue;
  }


  //------------------------ -o-
  assertExistenceOfReferringInstanceVariables  { | arrayOfVariableSymbols
                                                 |
    ^this.assertExistenceOfReferringInstanceElements(
            arrayOfVariableSymbols, true
     );
  }


  //------------------------ -o-
  assertExistenceOfReferringInstanceMethods  { | arrayOfMethodSymbols
                                               |
    ^this.assertExistenceOfReferringInstanceElements(
            arrayOfMethodSymbols, false
     );
  }




  //---------------------------------------------------------- -o--
  // Demo.

  *demo 
  {
    var  ipdc;

    Log.info(thisFunction, "BEGIN").postln; 
    IterationParameter.usage().postln;


    //
    ipdc = IterationParameterDemoClass.new();

    ipdc.demoIterationParameter.prretty().postln;

    Log.info(thisFunction, "ipdc.demoIterationParameter.pretty()...").postln; 
    ipdc.demoIterationParameter.pretty().postln;

    Log.info(thisFunction, "ipdc.demoIterationParameter.get()...").postln; 
    ipdc.demoIterationParameter.get().postln;

    Log.info(thisFunction, "ipdc.demoIterationParameter...").postln; 
    ipdc.demoIterationParameter.postln;


    //
    "".postln;
    Log.info(thisFunction, "ipdc.demoIterationParameter.set()").postln; 
    ipdc.demoIterationParameter.set(["Doce", "Trece"].choose);

    Log.info(thisFunction, "ipdc.demoIterationParameter.pretty()...").postln; 
    ipdc.demoIterationParameter.pretty().postln;


    //
    "".postln;
    Log.info(thisFunction, "ipdc.demoIterationParameter.iterate()").postln; 
    ipdc.demoIterationParameter.iterate();

    Log.info(thisFunction, "ipdc.demoIterationParameter.pretty()...").postln; 
    ipdc.demoIterationParameter.pretty().postln;

    Log.info(thisFunction, "ipdc.demoIterationParameter...").postln; 
    ipdc.demoIterationParameter.postln;

    Log.info(thisFunction, "ipdc.stringOfIterations...").postln; 
    ipdc.stringOfIterations.postln;


    //
    "".postln;
    Log.info(thisFunction, "ipdc.demoIterationParameter.valueReset()").postln; 
    ipdc.demoIterationParameter.valueReset();

    Log.info(thisFunction, "ipdc.demoIterationParameter.pretty()...").postln; 
    ipdc.demoIterationParameter.pretty().postln;

    Log.info(thisFunction, "ipdc.demoIterationParameter...").postln; 
    ipdc.demoIterationParameter.postln;


    //
    "".postln;
    Log.info(thisFunction, "ipdc.allowSetParameterBetweenIterations = true").postln; 
    ipdc.allowSetParameterBetweenIterations = true;

    Log.info(thisFunction, "ipdc.demoIterationParameter.set()").postln; 
    ipdc.demoIterationParameter.set(["Viente", "Trienta"].choose);
    
    Log.info(thisFunction, "ipdc.demoIterationParameter.pretty()...").postln; 
    ipdc.demoIterationParameter.pretty().postln;

    Log.info(thisFunction, "ipdc.demoIterationParameter.iterate()").postln; 
    ipdc.demoIterationParameter.iterate();

    Log.info(thisFunction, "ipdc.demoIterationParameter.pretty()...").postln; 
    ipdc.demoIterationParameter.pretty().postln;


    //
    "".postln;
    Log.info(thisFunction, "ipdc.allowSetParameterBetweenIterations = false").postln; 
    ipdc.allowSetParameterBetweenIterations = false;

    Log.info(thisFunction, "ipdc.isManualBecomesAutomaticallyActive = true").postln; 
    ipdc.isManualBecomesAutomaticallyActive = true;
    
    Log.info(thisFunction, "ipdc.demoIterationParameter.set()").postln; 
    ipdc.demoIterationParameter.set(["Ochenta", "Noventa"].choose);

    Log.info(thisFunction, "ipdc.demoIterationParameter.pretty()...").postln; 
    ipdc.demoIterationParameter.pretty().postln;

    Log.info(thisFunction, "ipdc.stringOfIterations...").postln; 
    ipdc.stringOfIterations.postln;

    Log.info(thisFunction, "ipdc.demoIterationParameter.iterate()").postln; 
    ipdc.demoIterationParameter.iterate();

    Log.info(thisFunction, "ipdc.demoIterationParameter.pretty()...").postln; 
    ipdc.demoIterationParameter.pretty().postln;

    Log.info(thisFunction, "ipdc.stringOfIterations...").postln; 
    ipdc.stringOfIterations.postln;


    //
    "".postln;
    Log.info(thisFunction, "ipdc.demoIterationParameter.iterate()").postln; 
    ipdc.demoIterationParameter.iterate();

    Log.info(thisFunction, "ipdc.demoIterationParameter.pretty()...").postln; 
    ipdc.demoIterationParameter.pretty().postln;

    Log.info(thisFunction, "ipdc.stringOfIterations...").postln; 
    ipdc.stringOfIterations.postln;


    //
    "".postln;
    Log.info(thisFunction, "END").postln; 

    '';
  }  // *demo




  //---------------------------------------------------------- -o--
  // MOS protocol methods.

  //------------------------ -o-
  //NB  Treat like primitive type.
  //
  //NB  Double spaces lost in .csminel()...
  //
  pretty  { | pattern, elisionLength, depth,
              casei, compileString, indent, initialCallee, 
              enumerate, bulletIndex, enumerationOffset,
              minimumSpacerSize, nolog, shortVersion
            |
    var  str  = value.pr(depth:0);

    str = ("% -> %").format(str, nextValue.pr(depth:0));

    if (isManual, { str = str ++ "  [M]"; });

    //
    if (str.regexpMatch(pattern, casei).not, { ^""; });

    //
    ^str;
  }


  //------------------------ -o-
  prretty  { | pattern, elisionLength, depth,
               casei, compileString, indent, initialCallee, 
               enumerate, bulletIndex, enumerationOffset,
               minimumSpacerSize, nolog, shortVersion
             |
    var  title  = this.classname.toUpper;

    var  maxKeyLength  = "valueResetFunction".size;

    var  enviroA1, enviroA2, enviroA3, 
         enviroB1,
         enviroC1, enviroC2, 
         enviroD1,
         enviroE1, enviroE2,
         enviroArray = []; 


    enviroA1 = (
      parameterName:            parameterName,
      value:                    value.pr(depth:0),
    );

    enviroA2 = (
      nextValue:                nextValue.pr(depth:0),
      valueClassType:           valueClassType,
    );

    enviroA3 = (
      isManual:                 isManual,
    );

                //SPACER

    enviroB1 = (
      allowSetParameterBetweenIterations:                       
                                allowSetParameterBetweenIterations.pr,
      isManualBecomesAutomaticallyActive: 
                                isManualBecomesAutomaticallyActive.pr,
    );

                //SPACER

    enviroC1 = (
      valueResetFunction:       valueResetFunction.pr(),
    );

    enviroC2 = (
      iterateFunction:          iterateFunction.pr(),
      parseValueFunction:       parseValueFunction.pr(),
    );

                //SPACER

    enviroD1 = (
      referringInstance:        referringInstance.pretty(nolog:true),
    );

                //SPACER

    enviroE1 = (
      referringVariables:       referringVariables,
    );

    enviroE2 = (
      referringMethods:         referringMethods,
    );



    enviroArray = [ enviroA1, enviroA2, enviroA3, (),
                    enviroB1, (),
                    enviroC1, enviroC2, (),
                    enviroD1, (),
                    enviroE1, enviroE2, 
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


  //ALIAS.
  prr  { | pattern, elisionLength, depth,
           casei, compileString, indent, initialCallee, 
           enumerate, bulletIndex, enumerationOffset,
           minimumSpacerSize, nolog, shortVersion
         |
    ^this.prretty( pattern, elisionLength, depth,
                   casei, compileString, indent, initialCallee,
                   enumerate, bulletIndex, enumerationOffset,
                   minimumSpacerSize, nolog,  false
          );
  }



  //------------------------ -o-
  *pretty  { | pattern, elisionLength, depth,
               casei, compileString, indent, initialCallee, 
               enumerate, bulletIndex, enumerationOffset,
               minimumSpacerSize, nolog, shortVersion
             |
    var  title  = this.classname.toUpper;

    var  maxKeyLength       = "".size;

    var  enviroA, 
         enviroArray = []; 


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


}  //IterationParameter




//---------------------------------------------------------- -o--
// For IterationParameter.demo().
//
IterationParameterDemoClass : Object  
{ 
  var  <>  isManualBecomesAutomaticallyActive, 
       <>  allowSetParameterBetweenIterations; 

  var  <   demoIterationParameter;

  var  <>  arrayOfIterationParameters;  
             // For actions on all IterationParameters.

  var  <>  stringOfIterations;



  //---------------------------------------------------------- -o--
  // Lifecycle.

  *new  {
    ^super.new.initIterationParameterDemoClass();
  }


  initIterationParameterDemoClass 
  {
    this.isManualBecomesAutomaticallyActive = false;
    this.allowSetParameterBetweenIterations = false;

    arrayOfIterationParameters = Array();

    this.demoIterationParameter = "ValorPrimer";
    this.stringOfIterations = "";

    ^this;
  }



  //---------------------------------------------------------- -o--
  // Setters for IterationParameters.

  demoIterationParameter_  { | value
                             |
    var  parseValueFunctionString,
         valueResetFunctionString,
         iterateFunctionString;


    // Define InstanceParameter functions.
    //
    parseValueFunctionString = { | self, refInst, incomingValue
                                 |
      self.newValue = nil;

      if (incomingValue.isNil, {
        incomingValue = self.valueResetFunction.().(self, refInst);
      });

      if (Parse.isInstanceOfClass(
                        self.parameterLogContext,
                        incomingValue, 
                        self.parameterName, String),
      {
        self.newValue = incomingValue;
      });

      //
      self.newValue.notNil;
    }.cs;


    //
    valueResetFunctionString = { | self, refInst
                                 |
      [ "Uno", "Dos", "Tres", "Cuatro", "Cinco", "Seis", "Siete" ].choose;
    }.cs;


    //
    iterateFunctionString = { | self, refInst
                              |
      refInst.stringOfIterations = refInst.stringOfIterations ++ self.value;
      '';
    }.cs;



    // Declare and set InstanceParameter.
    //

    if (demoIterationParameter.isNil, 
    {
      demoIterationParameter = IterationParameter(
        thisFunction,
        String,
        this,
  
        parseValueFunctionString,
        valueResetFunctionString,
        iterateFunctionString
      );

      arrayOfIterationParameters = 
        arrayOfIterationParameters.add(this.demoIterationParameter);
    });



    //
    demoIterationParameter.set(value);

    ^this;

  } //demoIterationParameter_

}  //IterationParameterDemoClass

