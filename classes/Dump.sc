//
// Dump.sc
//
// Dump values and data structures.
//
//
// PUBLIC METHODS--
//   *argsPaired  [*argsp *argsvp]
//   *args  *argsnl 
//   *what 
//
//   *classAnatomy 
//   *listOfMethods
//   *methodSignatures
//
//   *world
//
//   *sizeInBytes
//
// HELPER METHODS--
//   *collectionInColumns  
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//
//
// MOS DEPENDENCIES--
//   Collection_MOS
//   File_MOS
//   Log
//   Object_MOS
//   Parse
//   Platform_MOS
//   Server_MOS
//   String_MOS
//   Z
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------

Dump : MobileSound
{
  classvar   classVersion = "0.4";   //RELEASE



  //--------------------------------------------------------------- -o--
  *mosVersion { ^super.mosVersion(classVersion); }




  //--------------------------------------------------------------- -o--
  // Public class methods.

  //------------------------ -o-
  // No processing of key/value pairs in array.  
  // Just print with equal-spaced lvalue tokens.
  //
  *argsPaired  { | array, indent, minimumSpacerSize
                 |
    var  prefixString        = "\n" ++ (indent ?? "\t"),
         keySizeMin          = 0,
         separatorCharacter  = " ",
         separator           = "";

    var  str      = "",
         lastArg  = nil,
         empty    = "(empty)";


    //
    if (array.isNil || (array.size <= 0), { 
      ^"";
    });

    
    //
    forBy (0, (array.size - 1), 2, { |index|
      keySizeMin = max(keySizeMin, array[index].asString.size);
    });

    if (minimumSpacerSize.notNil, {
      keySizeMin = max(keySizeMin, minimumSpacerSize);
    });

    forBy (0, (array.size - 1), 2, { | index
                                     |
      if ((index+1) < array.size,  {
        lastArg = array[index + 1];
      }, {
        lastArg = empty;
      });

      separator = separatorCharacter
                    .dup(keySizeMin - array[index].asString.size).join; 

      str = str ++ ("%%%  = %")
              .format(prefixString, array[index], separator, lastArg);
    });


    //
    ^str;
    
  } // *argsPaired


  //ALIAS
  *argsp   { |array, indent|  ^this.argsPaired(array, indent, nil); }  

  *argsvp  { |...args|        ^this.argsPaired(args, nil, nil); }



  //------------------------ -o-
  *args  { |collection, logContext|  
    var  str  = "";
    collection.do { |elem|  str = str ++ ("%  ").format(elem); };
    ^Log.debug(logContext, str);
  }


  //
  *argsnl  { |collection, logContext|
    var  prefix  = "\n\t",
         str     = "";
    
    collection.do { |elem|  str = str ++ ("%%").format(prefix, elem); };

    ^Log.debug(logContext, str); 
  }


  //------------------------ -o-
  *what  { |symbolArray|
    var  enviro     = currentEnvironment,
         argsArray  = Array.new;

    var  symName, symValue;

    //
    symbolArray.do { |elem|
      symName = elem.asString;
      
      symValue = case
        { enviro.includesKey(elem); }
          { ("~" ++ elem.asString).compile.().csminel; }

        { elem.asString.size == 1; }
          { elem.asString.compile.().csminel; }
        
      ; //endcase

      argsArray = argsArray.addAll([ symName, symValue ]);
    };


    //
    ^Log.debug(nil, Dump.argsPaired(argsArray, nil, nil) );
  }



  //------------------------ -o-
  // INPUT: some Class
  //
  // NB  Defining pathPattern limits results to instance variables and methods.
  //
  *classAnatomy  { |pattern, casei, pathPattern, classobj|
    var  cn,
         collection,
         extensionSpecificCollection,
         displayInstanceOnly   = false,
         listing               = "",
         patternLabel          = nil,
         pathPatternLabel      = nil,
         searchLabel           = "",
         summary               = "",
         outputStringNotEmpty  = false;

    var  listCollection;

    var  str = "";


    //
    listCollection  = { | code, title, filterForFilepath
                        |
            //FUNCTION DEFAULTS.
            title                        = title              ?? code;
            filterForFilepath            = filterForFilepath  ?? false;
            extensionSpecificCollection  = nil;

            //
            collection = (cn ++ "." ++ code).compile.();

            if (filterForFilepath, {
              extensionSpecificCollection = 
                        collection.select { |elem|
                          classobj
                            .doesPatternMatchFilepathContainingInstanceMethod(
                               elem, 
                               pathPattern
                             );
                        };
            });

            if (extensionSpecificCollection.notNil, {
              collection = extensionSpecificCollection;
            });

            //
            listing = 
              Dump
                .collectionInColumns(collection, pattern:pattern, casei:casei);

            if (listing.notNil, {
              str = str ++ ("%%.%--\n%\n")
                               .format(searchLabel,
                                                cn.toUpper, title, listing);

              searchLabel = "";  // NB  Defined for first time only.
            });

            '';
          };


    // Sanity check.
    //
    if (classobj.isNil, {
      if (Dump == this, {
        classobj = Dump;
      }, {
        ^Log.error(thisFunction, "<classobj> is undefined.");
      });
    });

    cn = classobj.classname;


    //METHOD DEFAULTS.
    if (pathPattern.notNil, { 
      pathPatternLabel  = "\t\t\t\t(pathPattern: %)".format(pathPattern); 
      searchLabel       = searchLabel + pathPatternLabel;
    });

    if (pattern.notNil, { 
      patternLabel  = "\t\t\t\t(pattern: %)".format(pattern); 
      searchLabel   = searchLabel + patternLabel;
    });

    if (searchLabel.size > 0, { searchLabel = searchLabel + "\n" });

    pathPattern = if (pathPattern.isNil, { 
                    ".*"; 
                  }, { 
                    displayInstanceOnly = true;
                    pathPattern.asString; 
                  });


    //
    str = str ++ ("% -- %\n\n")
                     .format(classobj.classname.toUpper, classobj.summary);

    if (displayInstanceOnly.not, {
      listCollection.("classVarNames", filterForFilepath:false);

      listCollection.("class.methods.collect(_.name)", 
                         "class.methods", filterForFilepath:false);
    });

    listCollection.("instVarNames", filterForFilepath:true);

    listCollection.("methods.collect(_.name)", 
                       "methods", filterForFilepath:true);

    if (displayInstanceOnly.not, {
      listCollection.("subclasses", filterForFilepath:false);
    });


    if (str.size > 0, { outputStringNotEmpty = true; });

    str = str ++ (":: %").format(classobj.inheritanceString);

    if (outputStringNotEmpty, { str = "\n\n" ++ str ++ "\n"; });


    //
    ^str;

  }  // *classAnatomy



  //------------------------ -o-
  // INPUT: some Class
  //
  *listOfMethods  { |pattern, casei, pathPattern, classobj, classMethods=false|
    var  patternString,
         pathPatternString,
         displayInstanceOnly  = false;

    var  methodList,
         title  = "";

    var  str = "";


    //
    if (classobj.isNil, {
      if (Dump == this, {
        classobj = Dump;
      }, {
        ^Log.error(thisFunction, "<classobj> is undefined.");
      });
    });


    //DEFAULTS.
    patternString      = if (pattern.isNil, { ".*"; }, { pattern.asString; });

    pathPatternString  = if (pathPattern.isNil, { 
                           ".*"; 
                         }, { 
                           displayInstanceOnly = true;
                           pathPattern.asString; 
                         });

    casei = casei ?? true;


    //
    if (classMethods, {
      methodList = classobj.classname.asSymbol.asClass.class.methods;
      title = "class";
    }, {
      methodList = classobj.classname.asSymbol.asClass.methods;
      title = "instance";
    });

    if (methodList.isNil, { ^""; });

    title = ("% % methods--").format(classobj.classname.toUpper, title);


    //
    methodList.asList
      .sort({ |x,y|  x.asString < y.asString; })
        .do { |meth| 
            var  numargs               = meth.argNames.size - 1,
                 signature             = meth.name.asString ++ "(",
                 doesMatchPattern      = false,
                 doesMatchPathPattern  = false;

            meth.argNames.do({ |a, i|
              if (i > 0, { 			// NB  skip arg #1 (this)
                signature = signature ++ a.asString;
                if (i < numargs, { signature = signature ++ ", "; });
              });
            });
            
            signature = signature ++ ")";

            doesMatchPattern = 
              signature.asString.regexpMatch(patternString, casei);

            doesMatchPathPattern = 
                if (displayInstanceOnly, {
                    classobj.doesPatternMatchFilepathContainingInstanceMethod(
                               meth.name, pathPatternString
                             );
                }, {
                  true;
                });

            if (doesMatchPattern && doesMatchPathPattern, {
              str = str ++ ("  %\n").format(signature);
            });
          };
    
    //
    if (str.size > 0, { str = ("%\n%\n").format(title, str); });

    ^str;
  }


  //
  *methodSignatures  { |pattern, casei, pathPattern, classobj|   
    var  patternLabel         = "",
         pathPatternLabel     = "",
         searchLabel          = "",
         displayInstanceOnly  = false;

    var  outputStringNotEmpty  = false,
         str                   = "";


    //
    if (classobj.isNil, {
      if (Dump == this, {
        classobj = Dump;
      }, {
        ^Log.error(thisFunction, "<classobj> is undefined.");
      });
    });


    //
    str = str ++ ("% -- %\n\n")
                     .format(classobj.classname.toUpper, classobj.summary);

    if (pathPattern.notNil, { 
      pathPatternLabel     = "\t\t\t\t(pathPattern: %)".format(pathPattern); 
      searchLabel          = searchLabel + pathPatternLabel;
      displayInstanceOnly  = true;
    });

    if (pattern.notNil, { 
      patternLabel  = "\t\t\t\t(pattern: %)".format(pattern); 
      searchLabel   = searchLabel + patternLabel;
    });

    if (searchLabel.size > 0, { 
      str = str ++ searchLabel + "\n";
    });


    //
    if (displayInstanceOnly.not, {
      str = str ++ Dump.listOfMethods(
                                pattern, casei, pathPattern, classobj, true);
    });

    str = str ++ Dump.listOfMethods(
                                pattern, casei, pathPattern, classobj, false);


    //
    if (str.size > 0, { outputStringNotEmpty = true; });

    str = str ++ (":: %").format(classobj.inheritanceString);

    if (outputStringNotEmpty, { str = "\n\n" ++ str ++ "\n"; });

    ^str;
  }



  //------------------------ -o-
  // NB  Best effort!
  //
  *world {                                      //FORK
    var  filename,
         fileobj,
         //targz  = "tar cfvz ",
         gzip  = "gzip ",
         rval;

    var  verticalSpacer3  = "\n\n\n",
         verticalSpacer4  = "\n\n\n\n",
         separator        = 
                "---------------------------------------------------- -o-\n",
         str;

    //
    {
      filename  = File.pathToDesktopFile("sc-DUMPWORLD");
      fileobj   = File.open(filename, "a");
      gzip      = ("% %").format(gzip, filename);


      //
      str = ("%\n%\n%\n%\n%\n").format(
                "==================================================== -o--",
                "DUMP WORLD",
                ("  :: %").format(Date.localtime),
                ("  :: elapsed time: %").format(Date.elapsedTime),
                ("  :: % @ %").format(
                                 "whoami".unixCmdStdOut,
                                 "hostname".unixCmdStdOut
                               ),
                "===================================================="
                                      );
      str.post;
      fileobj.write(str);


      //
      str = Platform.pretty;
      str.post;
      fileobj.write(str);
                                                0.5.wait;

      //
      str = verticalSpacer3 ++ separator ++ Server.pretty;
      str.post;
      fileobj.write(str);
                                                0.5.wait;

      //
      (verticalSpacer4 ++ separator).postln;
      Server.nodes(queryControls:true);
                                                0.5.wait; 

      //
      str =    verticalSpacer3 ++ separator
            ++ Log.msg("ENVIRONMENT current \n%", 
                          Environment.current.pretty(nil, 1024, 999));
      str.post;
      fileobj.write(str);
                                                0.5.wait;

      //
      str = verticalSpacer4 ++ separator ++ Z.unixCmd(\ifconfig);
      fileobj.write(str);
                                                0.5.wait;

      str = verticalSpacer4 ++ separator ++ Z.unixCmd("netstat -ran");
      fileobj.write(str);
                                                0.5.wait;

      str = verticalSpacer4 ++ separator ++ Z.unixCmd("sysctl -a");
      fileobj.write(str);
                                                0.5.wait;

      //
      str = ("\n\n%\n%\n\n").format(
                "====================================================",
                "DONE."
                         );
      str.post;
      fileobj.write(str);


      //
      fileobj.flush;
      fileobj.close;


      //
      gzip.unixCmdStdOut;

      Log.info(thisFunction, "Created file \"%.gz\".", filename).postln; 

      '';

    }.fork;

    //
    ^'';

  } // *world



  //------------------------ -o-
  //NB  Values over 2**32 are evaluated as modules 2**32, 
  //    unless they are expressed exponential notation.
  //
  *sizeInBytes  { | numberValue
                  |
    var  precision  = 0.1;

    //
    if (Parse.isInstanceOfClass(thisFunction, 
                  numberValue, "numberValue", Number).not,      { ^nil; });

    //
    if (numberValue < 1024, { ^("%b").format(numberValue); });

    numberValue = numberValue / 1024.0;
    if (numberValue < 1024, { ^("%k").format(numberValue.round(precision)); });

    numberValue = numberValue / 1024.0;
    if (numberValue < 1024, { ^("%M").format(numberValue.round(precision)); });

    numberValue = numberValue / 1024.0;
    ^("%G").format(numberValue.round(precision));
  }




  //--------------------------------------------------------------- -o-
  // Helper methods.

  //------------------------ -o-
  // NB  collection object must have methods for .asList.sort.
  //     SymbolArray is known to work...
  //
  *collectionInColumns  { | collection, cols, pattern, casei
                          |
    var  patternString  = if (pattern.isNil, { ".*"; }, { pattern.asString; });

    var  caseInsensitiveMatch  = casei ?? true;

    var  list  = if (collection.notNil && (collection.size > 0), {
                   collection.select { |elem|
                     elem.asString
                       .regexpMatch(patternString, caseInsensitiveMatch);
                   }
                     .asList.sort({ |x,y|  x.asString < y.asString; });
                 });


    // NB  total line length: 
    //   (3*columnwidth) +  (2 + (2*separatorWidth)) + SUM(symbol_lengths>25)
    //
    var linewidth 	= 80, 	//DEFAULT
        columnwidth 	= 25, 	//DEFAULT
	columns 	= cols ?? (linewidth / columnwidth).floor,
	sections 	= (list.size / columns).floor,
	offset 		= list.size % columns,
	sectioncount 	= 1,
	addspacecount,
	currentElement,
        separatorWidth          = 7,
	spaceSeparator 	        = " ".dup(separatorWidth).join,
	spaceSeparatorInitial 	= "  ",  // two spaces
	index;

    var  str = "";


    //
    if (list.isNil || (list.size <= 0), { ^nil; });


    // adapt to other platforms...
    //
    if (Z.isSystemIOS, { 			// ios
      linewidth    = 160;       //DEFAULT
      columnwidth  = 30;        //DEFAULT

      columns   = cols ?? (linewidth / columnwidth).floor;
      sections  = (list.size / columns).floor;
      offset    = list.size % columns;
    });


    // dump bulk of list
    //
    sections.do({ |sect|
      str = str ++ spaceSeparatorInitial;
      addspacecount = 0;

      columns.do({ |col|
	currentElement = list[sect + (col*sections) + offset.min(col)].asString;
	str = str ++ currentElement;

        if (((col+1) < columns), {
	  addspacecount = addspacecount + columnwidth - currentElement.size;

	  if ((separatorWidth > addspacecount), { 
	    str = str ++ spaceSeparator;
	  }, {
            str = str ++ " ".dup(addspacecount).join; 
	    addspacecount = separatorWidth;
	  });
	});
      });

      str = str ++ "\n";
    });


    // dump the last line 
    //
    if ((0 != offset), {
      addspacecount = 0;
      str = str ++ spaceSeparatorInitial;

      offset.do({ |off| 
	index = sections + (off*sections) + offset.min(off);
	currentElement = list[index].asString;

	str = str ++ currentElement;

	if (((off+1) < columns), {
	  addspacecount = addspacecount + columnwidth - currentElement.size;

	  if ((separatorWidth > addspacecount), { 
	    str = str ++ spaceSeparator;
	  }, {
            str = str ++ " ".dup(addspacecount).join;
	    addspacecount = separatorWidth;
	  });
	});
      });

      str = str ++ "\n";
    });

    ^str;

  } // *collectionInColumns


}  // Dump

