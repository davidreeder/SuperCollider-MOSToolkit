//
// Parse.sc
//
// Sanity check the parsing of input arguments.
//
// NB  isOkayToBeNil is false by DEFAULT.
//
//
// PUBLIC METHODS--
//   *isInstanceOfClass
//   *isInstanceOfArrayOfTypeWithSize
//
// HELPER METHODS--
//   *isInstanceOfArrayOfClasses
//
//
// VALIDATOR FUNCTION STRING COMMON CASES--
//   *vfsMinimum
//   *vfsRange
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// DEPENDENCIES--
//   Log
//   Number_MOS
//   Object
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------

Parse : MobileSound
{
  classvar classVersion = "0.2";   //RELEASE




  //--------------------------------------------------------------- -o--
  // Lifecycle, identity, reflection.

  //------------------------ -o-
  *mosVersion { ^super.mosVersion(classVersion); }




  //--------------------------------------------------------------- -o--
  // Public class methods.

  //------------------------ -o-
  // logContext        thisFunction in calling context.
  // value             The value to be checked.
  // valueName         String representing the name of value.
  // classRequirement  A class or an Array of classes.
  // quiet             false: post reason for failure; true: no posting.
  //
  *isInstanceOfClass  { | logContext,                   //LOGS
                          value, 
                          valueName, 
                          classRequirement,
                          isOkayToBeNil,
                          quiet  
                        |
    var  okay    = false,
         oneStr  = "one ",
         classRequirementStr;


    //DEFAULTS.
    isOkayToBeNil  = isOkayToBeNil  ?? false;
    quiet          = quiet          ?? false;


    // Sanity check arguments.
    //
    if (Function != logContext.class, {
      Log.error( thisFunction, 
                 "logContext MUST be of class Function.  (%)", logContext
               ).postln; 
      ^false;
    });

    if (String != valueName.class, {
      Log.error( thisFunction, 
                 "valueName MUST be of class String.  (%)", valueName
               ).postln; 
      ^false;
    });

    if (Parse.isInstanceOfArrayOfClasses(thisFunction, classRequirement).not, 
                  { ^false; });

    if (quiet.isBoolean.not, {
      Log.error( thisFunction, 
            "quiet MUST be of class Boolean.  (%)", quiet).postln; 
      ^false;
    });



    // Sanity check value.
    //
    if (value.isNil, {
      if (isOkayToBeNil, { ^true; });

        Log.error(logContext, "% CANNOT be nil.", valueName).postln; 
        ^false;
    });


    //
    if (Array != classRequirement.class, {
      classRequirement = [classRequirement];
    });

    block  { |break|
      classRequirement.do({ |cr, index|
        okay  = case  
                  { Boolean == cr; }
                      { value.isBoolean; }

                  { Class == cr; }
                      { value.class.class == Class; }

                  { Number == cr; }
                      { value.isNumber; }

                  { Float == cr; }
                      { value.isFloat || value.isInteger; }

                  { SequenceableCollection == cr; }
                      { value.isSequenceableCollection; }

                  { SimpleNumber == cr; }
                      { (Integer == value.class) || (Float == value.class); }

                  { value.classname.asSymbol.asClass == cr; }

                  //ADD

                ; //endcase

        if (okay, { break.(); });
      });
    };

    if (okay, { ^true; });


    //
    classRequirementStr = classRequirement
                            .pretty(depth:0)
                            .replace("[", "").replace("]", "")
                            .stripWhiteSpace;

    if (classRequirement.size <= 1, { oneStr = ""; });

    if (quiet.not, {
      Log.error( logContext, 
                 "% value MUST be %of class %.  (%)", 
                    valueName, oneStr, classRequirementStr, value.prettyShort
               ).postln;
    });

    ^false;
  }


  //------------------------ -o-
  // Determine whether value is an object or array of objects exclusive
  // to classes defined by classRequirement, is of the proper 
  // size and where EACH ELEMENT IS VALIDATED by the Function
  // expressed in validatorFunctionString.  If minSize is 1, then an
  // array of one object may be expressed as an array with one
  // element or simply as an object of the proper class.  A single
  // element is validated in the same manner as elements of an Array.
  //
  // RETURN  true if value is compliant; false otherwise.
  //
  // NB  Per this.isInstanceOfClass, classRequirement may be a class 
  //     or an Array of classes.
  //
  // validatorFunctionString MUST compile() to a Function which
  // returns a Boolean per the validity of the tested element and
  // has the following signature: 
  //
  //   |logContext, value, valueName, classRequirement, validatorContextArray|
  //
  // validatorContextArray allows the calling environment to pass arbitrary 
  // objects required into the validatorFunction.
  //
  // Use verbose to provide a catch-all for failures in validatorFunction.  
  // However, production classes should include error logging within 
  // validatorFunctionString.
  //
  // NB  validatorFunction cannot take argumens directly.
  //     Always pass arguments through validatorContextArray.
  //
  *isInstanceOfArrayOfTypeWithSize  { | logContext,      //LOGS
                                        value,
                                        valueName,
                                        classRequirement,
                                        validatorFunctionString,
                                        validatorContextArray,
                                        size,
                                        minSize,
                                        maxSize,
                                        isOkayToBeNil,
                                        quiet,
                                        verbose
                                      |
    var  validatorFunction  = nil;


    //DEFAULTS.
    isOkayToBeNil  = isOkayToBeNil  ?? false;
    quiet          = quiet          ?? false;
    verbose        = verbose        ?? false;


    // Sanity check input arguments.
    //
    if (Parse.isInstanceOfClass(thisFunction, 
          logContext, "logContext", Function).not, { ^false; });

    if (Parse.isInstanceOfClass(thisFunction, 
          valueName, "valueName", String).not, { ^false; });

    //
    if (Parse.isInstanceOfArrayOfClasses(thisFunction, classRequirement).not, 
                  { ^false; });

    //
    if (size.notNil && (minSize.notNil || maxSize.notNil), {
      Log.error( thisFunction, 
                 "size is MUTUALLY EXCLUSIVE with minSize and maxSize."
               ).postln;
      ^false;
    });

    if (Parse.isInstanceOfClass(thisFunction, 
                size, "size", Integer, isOkayToBeNil:true).not, { ^false; });

    if (size.notNil, {
      if (size <= 0, {
        Log.error( thisFunction, 
                   "size MUST BE greater than zero.  (%)", size
                 ).postln; 
        ^false;
      });

      minSize = size;
      maxSize = size;
    });

    if (Parse.isInstanceOfClass(thisFunction, 
          minSize, "minSize", Integer, isOkayToBeNil:true).not, { ^false; });

    if (Parse.isInstanceOfClass(thisFunction, 
          maxSize, "maxSize", Integer, isOkayToBeNil:true).not, { ^false; });

    if (minSize.notNil, {
      if (minSize <= 0, {
        Log.error( thisFunction, 
                   "minSize MUST BE greater than zero.  (%)", minSize
                 ).postln; 
        ^false;
      });
    });

    if (maxSize.notNil, {
      if (minSize.notNil, {
        if (maxSize < minSize, {
          Log.error( thisFunction, 
                     "maxSize MUST BE greater than or equal to minSize.  " 
                        ++ "(minSize=%  maxSize=%)", 
                        minSize, maxSize
                   ).postln; 
          ^false;
        });

      }, {
        if (maxSize <= 0, {
          Log.error( thisFunction, 
                     "maxSize MUST BE greater than zero.  (%)", maxSize
                   ).postln; 
          ^false;
        });
      });
    });

    //
    if (validatorFunctionString.notNil, 
    {
      if (Parse.isInstanceOfClass(thisFunction, 
                  validatorFunctionString, "validatorFunctionString", 
                    String).not, { ^false; });

      validatorFunction = validatorFunctionString.compile();

      if (Parse.isInstanceOfClass(thisFunction,
                  validatorFunction, "validatorFunction", Function).not, 
                    { ^false; });
    });

    if (Parse.isInstanceOfClass(thisFunction, 
                  validatorContextArray, "validatorContextArray", Array, 
                  isOkayToBeNil:true).not, 
                    { ^false; });

    //
    if (Parse.isInstanceOfClass(thisFunction, 
                  quiet, "quiet", Boolean).not, { ^false; });

    if (Parse.isInstanceOfClass(thisFunction, 
                  verbose, "verbose", Boolean).not, { ^false; });



    // Sanity check value.
    //
    if (value.isNil, 
    {
      if (isOkayToBeNil, { ^true; });

      if (quiet.not, {
        Log.error(logContext, "% CANNOT be nil.", valueName).postln; 
      });

      ^false;
    });

    value = value.intoArray;


    //
    if (minSize.notNil && maxSize.notNil, 
    {
      if ((value.size < minSize) || (value.size > maxSize), 
      {
        if (quiet.not, {
          if (minSize == maxSize, {
            Log.error( logContext, 
                       "% array size MUST be exactly %.  (%)",
                          valueName, minSize, value.prettyShort
                     ).postln; 
          }, {
            Log.error( logContext, 
                       "% array size MUST be in range [%, %].  (%)",
                          valueName, minSize, maxSize, value.prettyShort
                     ).postln; 
          });
        });

        ^false;
      });

    }, {
      if (minSize.notNil, {
        if (value.size < minSize, 
        {
          if (quiet.not, {
            Log.error( logContext, 
                       "% array size MUST be no less than %.  (%)",
                          valueName, minSize, value.prettyShort
                     ).postln; 
          });

          ^false;
        });
      });

      if (maxSize.notNil, {
        if (value.size > maxSize, 
        {
          if (quiet.not, {
            Log.error( logContext, 
                       "% array size MUST be no greater than %.  (%)",
                          valueName, maxSize, value.prettyShort
                     ).postln; 
          });

          ^false;
        });
      });
    });

    if ((value.size <= 0) && isOkayToBeNil.not, 
    {
      if (quiet.not, {
        Log.error(thisFunction, "value array CANNOT be of size zero.").postln; 
      });

      ^false;
    });

    //
    value.do({ |element, index|
      var  valueNameIndex  = ("%[%]").format(valueName, index);

      if (Parse.isInstanceOfClass(logContext, 
            element, valueNameIndex, classRequirement).not, { ^false; });

      if (validatorFunctionString.notNil, {
        if (validatorFunction
              .()
              .(logContext, 
                  element, 
                  valueNameIndex, classRequirement, validatorContextArray)
              .not,
        {
          if (verbose, {
            Log.error( thisFunction, 
                       "Array element FAILED validatorFunction.  (%=%)", 
                          valueNameIndex, element.prettyShort
                     ).postln; 
          });

          ^false;
        });
      });
    });


    //
    ^true;
  }




  //--------------------------------------------------------------- -o--
  // Helper methods.

  //------------------------ -o-
  // NB  No quiet switch in the helper.
  //
  *isInstanceOfArrayOfClasses  { | logContext, classRequirement    //LOGS
                                 |
    if (classRequirement.isNil, {
      Log.error(logContext, "classRequirement CANNOT be nil.").postln; 
      ^false;
    });

    if (classRequirement.isArray.not, {
      if (classRequirement.isClass.not, {
        Log.error( logContext, 
                   "classRequirement MUST be a class object "
                      ++ "or an Array of class objects.  (%)",
                            classRequirement.prettyShort
                 ).postln; 
        ^false;
      });

    }, {
      if (Parse.isInstanceOfClass(logContext, 
            classRequirement, "classRequirement", Array).not, { ^false; }); 

      if (classRequirement.size <= 0, 
      {
        Log.error( logContext, 
                   "classRequirement CANNOT be an empty Array."
                 ).postln; 
        ^false;
      });


      classRequirement.do({ | element, index
                            |
        if (element.isClass.not, {
          var  elementIndexString  = ("classRequirement[%]").format(index);

          Log.error( logContext, 
                     "Elements of classRequirement MUST "
                        ++ "be of type Class.  (%=%)",
                              elementIndexString, element.prettyShort
                   ).postln;  

          ^false;
        });
      });
    });

    ^true;
  }




  //--------------------------------------------------------------- -o--
  // Validator Function String common cases.

  //------------------------ -o-
  // minValue = validatorContextArray[0];
  //
  *vfsMinimum
  { 
     ^{ | logContext, 
          value, valueName, 
          classRequirement, validatorContextArray
        |
        var  minValue,
             rval      = false;

        //
        case
          { validatorContextArray.isNil; }
              {
                Log.error(logContext, 
                         "validatorContextArray MUST must contain "
                      ++ "a single Number.  (%)", 
                            validatorContextArray
                    ).postln; 
              }

          { minValue = validatorContextArray[0];
            minValue.isNumber.not; 
          }
              {
                Log.error(logContext, 
                      "minValue MUST be of class Number.  (%)", minValue.class
                    ).postln; 
              }

          { value < minValue; }
              {
                Log.error(logContext, 
                      "% MUST be GREATER THAN OR EQUAL TO %.  (%)",
                          valueName, minValue, value
                    ).postln; 
              }
  
          { rval = true; }
  
        ; //endcase
        
        //
        rval;
      }.cs;
  }


  //------------------------ -o-
  // minValue = validatorContextArray[0];
  // maxValue = validatorContextArray[1];
  //
  *vfsRange  
  { 
     ^{ | logContext, 
          value, valueName, 
          classRequirement, validatorContextArray
        |
        var  minValue, maxValue,
             rval      = false;

        //
        case
          { validatorContextArray.isNil 
              || validatorContextArray.size != 2;
          }
              {
                Log.error(logContext, 
                         "validatorContextArray MUST must contain "
                      ++ "a pair of Numbers.  (%)", 
                            validatorContextArray
                    ).postln; 
              }

          { minValue = validatorContextArray[0];
            maxValue = validatorContextArray[1];

            (minValue.isNumber && maxValue.isNumber).not;
          }
              {
                Log.error(logContext, 
                         "minValue and maxValue MUST "
                      ++ "be of class Number.  ([%, %])", 
                            minValue.class, maxValue.class
                    ).postln; 
              }

          { (minValue <= maxValue).not; }
              {
                Log.error(logContext, 
                         "minValue MUST be LESS THAN OR EQUAL TO "
                      ++ "maxValue. ([%, %])", minValue, maxValue
                    ).postln; 
              }

          { value.inRange(minValue, maxValue).not; }
              {
                Log.error(logContext, 
                      "% MUST be IN RANGE (%,%).  (%)",
                          valueName, minValue, maxValue, value
                    ).postln; 
              }
  
          { rval = true; }
  
        ; //endcase
        
        //
        rval;
      }.cs;
  }

} // Parse

