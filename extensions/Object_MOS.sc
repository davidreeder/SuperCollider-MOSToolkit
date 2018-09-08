//
// Object_MOS.sc
//
//
// PUBLIC METHODS--
//   *mosVersion
//
//   (*)classname
//   *filename  *filenameQuoted
//   (*)mosFilename
//
//   (*)isInstance
//
//   (*)inheritanceString  
//   (*)summary
//   (*)methodSignatures  [(*)sigs]
//     (*)sigsmos  (*)sigsjit
//   (*)classAnatomy  [(*)cla]
//     (*)clamos  (*)clajit 
//
//   (*)moshelp
//
//   (*)openTerminal
//
//   (*)hasMethod  (*)hasClassMethod  (*)hasInstanceMethod 
//   (*)hasRespondingMethod  (*)hasRespondingInstanceMethod
//   (*)respondingInstanceMethod
//
//   *hasVariableTest
//   (*)hasVariable  (*)hasClassVariable  (*)hasInstanceVariable
//
//   (*)doesPatternMatchFilepathContainingInstanceMethod
//   *recurseClassesWithFunction
//
//   compileStringMinimizeElide  [csminel]
//   minel
//
//   isBoolean
//   isBus
//   isClass
//   isCurve
//   isSimpleArray
//   isSymbol
//
//   intoArray
//
//
// GENERAL HELPER METHODS--
//   (*)labelEmpty
//   (*)labelNil
//   (*)labelIncomplete
//   (*)labelUndefined
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersionString
//   pretty  prretty  [(*)pr (*)prr]
//   prettyShort  prrettyShort
//   prettyLocal
//
//
// MOS DEPENDENCIES--
//   Dump
//   Environment_MOS
//   Curve
//   Log
//   Parse
//   String_MOS
//   Z
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Object  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.2";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--

  //--------------------------- -o-
  *classname  { ^this.class.asString.replace("Meta_", ""); }
  classname   { ^this.class.asString; }


  //
  *filename        { ^this.filenameSymbol.asString; }
  *filenameQuoted  { ^this.filenameSymbol.asString.shellQuote; }


  //
  *mosFilename  {
    var  mosMethodArray  = 
           this.class.methods.select({ |elem|  \mosVersion == elem.name; });

    if (mosMethodArray.size != 1, { ^nil; });

    ^mosMethodArray[0].filenameSymbol.asString;
  }

  mosFilename  { ^this.class.mosFilename; }



  //--------------------------- -o-
  *isInstance  { ^false; }
  isInstance   { ^true; }



  //--------------------------- -o-
  *inheritanceString  { 
    var  str = this.classname;
    this.superclasses.do({ |c|  str = str ++ (": %").format(c); });
    ^str;
  }

  inheritanceString  { ^this.class.inheritanceString; }



  //--------------------------- -o-
  *summary  {
    var  documentKey  = ("Classes/%").format(this.classname);
    ^SCDoc.documents.at(documentKey).summary;
  }

  summary  { ^this.class.summary; }


  //--------------------------- -o-
  *methodSignatures  	{ | pattern, casei, pathPattern
                          |
    ^Dump.methodSignatures( pattern:      pattern, 
                            casei:        casei, 
                            pathPattern:  pathPattern, 
                            classobj:     this
                          ); 
  }

  methodSignatures      { |pattern, casei, pathPattern|  
    ^this.class.methodSignatures(pattern, casei, pathPattern);
  }  


  // ALIAS
  *sigs  { |pattern, casei, pathPattern|  
    ^this.methodSignatures(pattern, casei, pathPattern); 
  }          
  sigs   { |pattern, casei, pathPattern|
    ^this.class.methodSignatures(pattern, casei, pathPattern); 
  }


  // ALIAS
  *sigsmos  { |pattern, casei|  
    ^this.methodSignatures(
            pattern:pattern, casei:casei, pathPattern:"_MOS\.sc$"); 
  }          

  sigsmos   { |pattern, casei|
    ^this.class.methodSignatures(
                  pattern:pattern, casei:casei, pathPattern:"_MOS\.sc$"); 
  }


  // ALIAS
  *sigsjit  { |pattern, casei|  
    ^this.methodSignatures(
            pattern:pattern, casei:casei, pathPattern:"/JITLib/"); 
  }          

  sigsjit  { |pattern, casei|
    ^this.class.methodSignatures(
                  pattern:pattern, casei:casei, pathPattern:"/JITLib/"); 
  }



  //--------------------------- -o-
  *classAnatomy  { |pattern, casei, pathPattern|  
    ^Dump.classAnatomy( pattern:      pattern, 
                        casei:        casei, 
                        pathPattern:  pathPattern, 
                        classobj:     this
                      ); 
  }

  classAnatomy   { |pattern, casei, pathPattern|  
    ^this.class.classAnatomy(pattern, casei, pathPattern); 
  }  


  //ALIAS
  *cla  { |pattern, casei, pathPattern|  
    ^this.classAnatomy(pattern, casei, pathPattern); 
  }         

  cla   { |pattern, casei, pathPattern|  
    ^this.class.classAnatomy(pattern, casei, pathPattern); 
  }  


  //
  *clamos  { |pattern, casei|  
    ^this.classAnatomy(pattern:pattern, casei:casei, pathPattern:"_MOS\.sc$"); 
  }         

  clamos   { |pattern, casei|  
    ^this.class.classAnatomy(   
                       pattern:pattern, casei:casei, pathPattern:"_MOS\.sc$" ); 
  }  


  //
  *clajit  { |pattern, casei|  
    ^this.classAnatomy(pattern:pattern, casei:casei, pathPattern:"/JITLib/"); 
  }         

  clajit   { |pattern, casei|  
    ^this.class.classAnatomy(   
                       pattern:pattern, casei:casei, pathPattern:"/JITLib/" ); 
  }  



  //--------------------------- -o-
  *moshelp  { ^Z.moshelp(this.classname, patternAsClass:true); }

  moshelp  { |pattern, index, patternAsClass, folders|
  
    if (this.isString || (Symbol == this.class), {
      ^Z.moshelp(this, index, patternAsClass, folders);
    });

    ^this.class.moshelp(this, patternAsClass:true); 
  }



  //--------------------------- -o-
  *openTerminal  { 
    Z.openTerminal(this.asSymbol.asClass.filenameQuoted); ^""; 
  }

  openTerminal   { ^this.class.openTerminal; } 



  //--------------------------- -o-
  // RETURN: Boolean
  //
  *hasClassMethod  { | methodName
                     |
    var  arrayOfClassMethods = 
           (this.classname ++ ".class.methods.collect(_.name)").compile.();

    //
    if (methodName.isNil, { 
      Log.error(thisFunction, "<methodName> is nil.").postln;
      ^false;
    });


    //
    if (arrayOfClassMethods.notNil, {
      ^arrayOfClassMethods.includes(methodName.asSymbol);
    });

    ^false;
  }


  hasClassMethod  { |methodName|  ^this.class.hasClassMethod(methodName); }



  //--------------------------- -o-
  // RETURN: Boolean
  //
  *hasInstanceMethod  { | methodName
                        |
    var  arrayOfMethods = 
             (this.classname ++ ".methods.collect(_.name)").compile.();

    if (methodName.isNil, { 
      Log.error(thisFunction, "<methodName> is nil.").postln;
      ^false;
    });


    //
    if (arrayOfMethods.notNil, {
      ^arrayOfMethods.includes(methodName.asSymbol);
    });

    ^false;
  }


  hasInstanceMethod  { |methodName|  
    ^this.class.hasInstanceMethod(methodName); 
  }


  //--------------------------- -o-
  *hasMethod  { | methodName
                |  
    ^(    this.hasInstanceMethod(methodName) 
       || this.hasClassMethod(methodName) 
     );
  }

  hasMethod  { |methodName|  
    ^this.class.hasMethod(methodName);
  }


  //--------------------------- -o-
  *hasRespondingMethod  { |methodName| 
    ^this.respondsTo(methodName);
  }

  hasRespondingMethod  { |methodName| 
    ^this.class.hasRespondingMethod(methodName);
  }


  //--------------------------- -o-
  // ASSUME  .findRespondingMethodFor returns instance methods only.
  //
  *hasRespondingInstanceMethod  { |methodName|
    var  rval  = this.findRespondingMethodFor(methodName);

    if (this.hasClassMethod(methodName) || rval.notNil, { ^true; });

    ^false;
  }

  hasRespondingInstanceMethod  { |methodName|
    ^this.class.hasRespondingInstanceMethod(methodName); 
  }


  //--------------------------- -o-
  *respondingInstanceMethod  { |methodName| 
    ^this.findRespondingMethodFor(methodName);
  }

  respondingInstanceMethod  { |methodName| 
    ^this.class.respondingInstanceMethod(methodName);
  }


  //--------------------------- -o-
  *hasVariableTest  { | variableName, casei, 
                        testForInstanceVariable
                      |
    var  listOfVariables          = nil;
    var  listOfDetectedVariables  = nil;


    //DEFAULTS.
    casei                    = casei                    ?? true; 
    testForInstanceVariable  = testForInstanceVariable  ?? true;

    variableName = variableName !? variableName.asString;

    if (Parse.isInstanceOfClass(thisFunction, 
              variableName, "variableName", String).not, { ^false; });


    //
    if (testForInstanceVariable, {
      listOfVariables = this.instVarNames;
    }, {
      listOfVariables = this.classVarNames;
    });


    listOfDetectedVariables = 
      listOfVariables.detect({ |varname|  
        varname.asString.regexpMatch("^" ++ variableName ++ "$", casei); 
      });

    if (listOfDetectedVariables.isNil, { ^false; });

    ^true;
  }


  //--------------------------- -o-
  *hasVariable  { |variableName, casei|
    ^(    this.hasClassVariable(variableName, casei)
       || this.hasInstanceVariable(variableName, casei) );
  }

  hasVariable  { |variableName, casei|
    ^this.class.hasVariable(variableName, casei);
  }

  //
  *hasClassVariable  { |variableName, casei|
    ^this.hasVariableTest(variableName, casei.(), false);  // XXX ?!
  }

  hasClassVariable  { |variableName, casei|
    ^this.class.hasClassVariable(variableName, casei);
  }

  //
  *hasInstanceVariable  { |variableName, casei|
    ^this.hasVariableTest(variableName, casei.(), true);  // XXX ?!
  }

  hasInstanceVariable  { |variableName, casei|
    ^this.class.hasInstanceVariable(variableName, casei.());
  }



  //--------------------------- -o-
  // RETURN:  Value of case-insensitive search with <pattern> against 
  //          the pathname of class object containing instance <method>.
  //
  // The more precise is pattern, the better the result.
  //
  *doesPatternMatchFilepathContainingInstanceMethod  { |method, pattern|

    var  methodObj,
         classnamePath  = "";


    // 
    if (method.isNil || pattern.isNil, {
      ^Log.error(thisFunction, "<method> or <pattern> is nil.");
    });

    method = method.asSymbol;


    //
    methodObj = this.respondingInstanceMethod(method);

    if (methodObj.isNil, { 
      if (this.hasClassMethod(method), {
        Log.warning(thisFunction, "\"%\" is class method!", method).postln; 
      });

      ^false; 
    });


    classnamePath = methodObj.filenameSymbol.asString;

    if (classnamePath.regexpMatch(pattern, casei:false), {
      ^true;
    }, {
      ^false;
    });
  }


  //
  doesPatternMatchFilepathContainingInstanceMethod  { |method, pattern|
    ^this.class
            .doesPatternMatchFilepathContainingInstanceMethod(method, pattern); 
  }



  //--------------------------- -o-
  // Function signature:
  //
  //   f(thisClass, indentString, indentWidth, indentWidthIncrement)
  //
  // All input arguments may be ignored, but they must be received.
  //
  // Function MUST RETURN a boolean.  Return true for thisClass to be 
  // included in in sorted array returned to calling environment.
  //
  *recurseClassesWithFunction  { |    function,
                                      indentString          = " ",
                                      indentWidth           = 0, 
                                      indentWidthIncrement  = 2
                                 |

    var  arrayOfSubclasses       = 
                  (this.classname ++ ".subclasses").compile.(),
         arrayOfMatchingClasses  = Array.new,
         functionReturnValue     = false,
         elemMatch               = nil;


    //
    if (function.isNil, { 
      Log.error(thisFunction, "<function> is nil.").postln;
      ^nil;
    });

    if (Class == this, { ^nil; });  // XXX  Ignores class Class?!

    // NBXXX  Triggered somehow by (eg) Boolean.
    //
    if ("^Meta_".matchRegexp(this.asString), { 
      Log.warning(thisFunction, 
                    "this is Meta_ class.  (%)", this.classname).postln; 
      ^nil; 
    });


    //
    functionReturnValue = function.(  
                            this,
                            indentString,
                            indentWidth,
                            indentWidthIncrement,
                          );

    if (functionReturnValue, {
      arrayOfMatchingClasses = arrayOfMatchingClasses.addAll(this);
    });


    //
    if (arrayOfSubclasses.size <= 0, { ^arrayOfMatchingClasses; });

    arrayOfSubclasses = 
      arrayOfSubclasses.asList.sort({ |x,y|  x.asString < y.asString; });

    arrayOfSubclasses.asArray.do { |elem|  
      elemMatch = elem.recurseClassesWithFunction(
                         function, 
                         indentString,
                         indentWidth+indentWidthIncrement, 
                         indentWidthIncrement
                       );

      if (elemMatch.notNil && (elemMatch.size > 0), { 
        arrayOfMatchingClasses = arrayOfMatchingClasses.addAll(elemMatch); });
    };


    //
    ^arrayOfMatchingClasses.flat.sort({ |x, y| x.asString < y.asString; });

  }  // *recurseClassesWithFunction



  //--------------------------- -o-
  compileStringMinimizeElide  { | elisionLength, compileString
                                |
    var  str  = "";


    //DEFAULTS.
    compileString = compileString ?? true;

    //
    if (this.isCollection || this.isFunction, {
      if (compileString, { 
        ^this.cs.minimize.elide(elisionLength);
      }, {
        ^this.asString.minimize.elide(elisionLength);
      });
    });

    str = if (compileString, { this.cs; }, { this.asString; });

    ^str.stripWhiteSpace;
  }


  //ALIAS
  csminel  { |elisionLength|  
    ^this.compileStringMinimizeElide(elisionLength);
  }



  //--------------------------- -o-
  minel  { |elisionLength|  ^this.minimize.elide(elisionLength); }


  //--------------------------- -o-
  isBoolean  {
    if ( (True == this.class) || (False == this.class), { ^true; }); 
    ^false;
  }

  isBus  { ^(this.class == Bus); }

  isClass  { ^this.class.asSymbol.isMetaClassName; }

  isCurve  { ^(this.class == Curve); }

  isSimpleArray  { ^(this.class == Array); }

  isSymbol  { ^(Symbol == this.classname.asSymbol.asClass); }


  //--------------------------- -o-
  intoArray  { 
    if (this.isSimpleArray, { ^this; });
    ^[ this ];
  }




  //------------------------------------------- -o--
  // General helper methods.

  //--------------------------- -o-
  *labelEmpty  { ^"(empty)"; }                         //XXX
  labelEmpty   { ^this.class.labelEmpty; }

  //
  *labelIncomplete  { ^"(incomplete)"; }               //XXX
  labelIncomplete   { ^this.class.labelIncomplete; }

  //
  *labelNil  { ^"(nil)"; }                             //XXX
  labelNil   { ^this.class.labelNil; }

  //
  *labelUndefined  { ^"(undefined)"; }                 //XXX
  labelUndefined   { ^this.class.labelUndefined; }




  //------------------------------------------- -o--
  // MOS protocol methods.

  //--------------------------- -o-
  *mosVersionString  { | extensionVersion     = "-1",
                         extensionNameSuffix  = "_MOS"
                       |
    ^("%% v%").format(this.asString, extensionNameSuffix, extensionVersion);
  }


  //--------------------------- -o-
  // Default definition of .pretty().  
  // Falls back to .asString.
  //
  // NB  Do not .minel asString result when compileString is false.
  //
  // NB  Only Parse arguments that are used in this default case.
  //     Let .prettyLocal() Parse and check all arguments.
  //
  // NB  Currently only the aliases have class versions.  [why?]
  //
  pretty  {     | pattern, elisionLength, depth,
                  casei, compileString, indent, initialCallee, 
                  enumerate, bulletIndex, enumerationOffset,
                  minimumSpacerSize, nolog, shortVersion
                |
    //DEFAULTS.
    compileString = compileString ?? true;

    if (Parse.isInstanceOfClass(thisFunction, 
          compileString, "compileString", Boolean).not, { ^nil; });


    //
    if (this.asString.regexpMatch(pattern, casei).not, { ^""; });

    if (compileString, { ^this.csminel(elisionLength); });

    ^this.asString;
  }

  //
  prretty  {     | pattern, elisionLength, depth,
                   casei, compileString, indent, initialCallee, 
                   enumerate, bulletIndex, enumerationOffset,
                   minimumSpacerSize, nolog, shortVersion
                |
    ^this.pretty(  pattern, elisionLength, depth,
                   casei, compileString, indent, initialCallee, 
                   enumerate, bulletIndex, enumerationOffset,
                   minimumSpacerSize, nolog,  false
          );
  }


  //ALIAS
  pr  { | pattern, elisionLength, depth,
          casei, compileString, indent, initialCallee, 
          enumerate, bulletIndex, enumerationOffset,
          minimumSpacerSize, nolog, shortVersion
        |
    ^this.pretty( pattern, elisionLength, depth,
                  casei, compileString, indent, initialCallee, 
                  enumerate, bulletIndex, enumerationOffset,
                  minimumSpacerSize, nolog,  true
          );
  }

  //
  prr  { | pattern, elisionLength, depth,
           casei, compileString, indent, initialCallee, 
           enumerate, bulletIndex, enumerationOffset,
           minimumSpacerSize, nolog, shortVersion
         |
    ^this.pretty( pattern, elisionLength, depth,
                  casei, compileString, indent, initialCallee, 
                  enumerate, bulletIndex, enumerationOffset,
                  minimumSpacerSize, nolog,  false
          );
  }

  //CLASS
  *pr  { | pattern, elisionLength, depth,
           casei, compileString, indent, initialCallee, 
           enumerate, bulletIndex, enumerationOffset,
           minimumSpacerSize, nolog, shortVersion
         |
    ^this.pretty( pattern, elisionLength, depth,
                  casei, compileString, indent, initialCallee, 
                  enumerate, bulletIndex, enumerationOffset,
                  minimumSpacerSize, nolog,  true
          );
  }

  //
  *prr  { | pattern, elisionLength, depth,
            casei, compileString, indent, initialCallee, 
            enumerate, bulletIndex, enumerationOffset,
            minimumSpacerSize, nolog, shortVersion
          |
    ^this.pretty( pattern, elisionLength, depth,
                  casei, compileString, indent, initialCallee, 
                  enumerate, bulletIndex, enumerationOffset,
                  minimumSpacerSize, nolog,  false
          );
  }


  //------------------------ -o-
  // Minimizes .pretty() output to a single, truncated line.
  //
  //NB  Currently no class versions for these.
  //
  prettyShort  {
    ^this.pretty(depth:0, nolog:true, shortVersion:true)
         .minel(elisionLength:32);   //XXX
  }

  prrettyShort  {
    ^this.pretty(depth:0, nolog:true, shortVersion:false)
         .minel(elisionLength:32);   //XXX
  }


  //------------------------ -o-
  // Called by classes that implement their own version of .pr(r)etty().
  //
  // INPUT:
  //   enviroArray        
  //       Array of "environments" to be printed, in turn.
  //   title              
  //       Title of this pretty() dump.
  //   pattern            
  //       Capture only lines containing this pattern, if defined.
  //   elisionLength      
  //       Truncate final result to elisionLength.
  //   depth              
  //       Apply pretty() to this depth, formatting complex inner elements.
  //   casei              
  //       pattern match is case insensitive (true), or case sensitive (false).
  //   compileString      
  //       If true, send compileString to environment elements. 
  //   indent             
  //       Indent every line by String indent.
  //   initialCallee      
  //       If true, caller is the first to invoke pretty().  (depth == 0)
  //   enumerate          
  //       Pass this boolean, some forms of pretty() will use it...
  //   bulletIndex        
  //       Choose rotating bullet character.  (Used in Dictionary.pretty().)
  //   enumerationOffset  
  //       UNUSED?!
  //   minimumSpacerSize  
  //       UNUSED?!
  //   nolog              
  //       Omit timestamp from this pretty() dump.
  //   shortVersion       
  //       If true, an enviroArray of smaller size has been used.
  //
  //
  // For internal use only: bulletIndex, indent, initialCallee.
  // See also String.elide(), Dictionary.ment()
  //
  // NB  Odd results below depth when .pretty, .asString and .cs differ.
  //     Should always be able to search on object name.
  //
  prettyLocal  {     | enviroArray, title,
                       pattern, elisionLength, depth,
                       casei, compileString, indent, initialCallee, 
                       enumerate, bulletIndex, enumerationOffset,
                       minimumSpacerSize, nolog, shortVersion
                     |
    var  indentLocal              = " ".dup(3).join,
         enviroArrayTotalSize     = -1,
         noMatchLabel             = "\n\t(no match)";

    var  thresholdForPostingElementCount  = nil,
         elementCountStr                  = "";

    var  str;


    //DEFAULTS.
    depth          = depth          ?? 1;
    initialCallee  = initialCallee  ?? true;
    nolog          = nolog          ?? false;

    thresholdForPostingElementCount =
                         thresholdForPostingElementCount ?? 3;   //XXX

    if (this.isClass(), {
      title = title ++ " CLASS";
    });

    if (initialCallee, { 
      indent = indent ?? "\t";
    }, {
      indent = (indent ?? "") ++ indentLocal; 
    });


    // Process.
    // Empty environment objects trigger a newline separator.
    //
    enviroArray.do { |elem|
      if (elem.size <= 0, {
        if (elem.notNil, {
          if (str.size > 0, { str = str ++ "\n"; });
        });

      }, {
        str = str ++ elem.ment(  
                       pattern:            pattern, 
                       elisionLength:      elisionLength, 

                       casei:              casei, 
                       compileString:      compileString, 
                       indent:             indent,
                       initialCallee:      false, 

                       title:              nil,
                       minimumSpacerSize:  minimumSpacerSize,
                     );
      });
    };


    // Format case.
    //
    if (initialCallee, {
      if (nolog, {
        ^("%  %").format(title, str);

      }, {
        str = if (str.size <= 0, { 
          if (enviroArray.size <= 0, { ""; }, { noMatchLabel; }); 
        }, { 
          str; 
        });

        ^("\n%%").format( Log.msg(title), str );
      })
    });


    if (depth > 0,
    {
      enviroArrayTotalSize = enviroArray.sum(_.size);
      if (enviroArrayTotalSize >= thresholdForPostingElementCount, {
        elementCountStr = ("\t\t\t\t-- %").format(enviroArrayTotalSize);
      });
    });
    
    if ( (str.size > 0) 
            || (title.notNil && title.regexpMatch(pattern, casei)), {

      str = ("%: (%%\n%)").format(title, elementCountStr, str, indent );
    });

    if (depth <= 0, {
      ^str.minimize.elide(elisionLength);
    });

    ^str;

  } //prettyLocal

} //Object

