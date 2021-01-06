//
// Dictionary_MOS.sc
//
//
// PUBLIC METHODS--
//   ment
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//   pretty
//
//
// MOS DEPENDENCIES--
//   Dump
//   Object_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ Dictionary  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }




  //------------------------------------------- -o--
  // Instance methods.

  //--------------------- -o-
  // As in: "print environMENT".
  //
  //NB  Slightly different signature than .pretty/.prettyLocal in Object.
  //
  ment  {       | pattern, elisionLength, 
                  casei, compileString, indent, initialCallee, 
                  title, minimumSpacerSize
                |

    var  enviroKeys   = this.keys.asList.sort,
         enviroValue  = "",
         keyValue,
         matchKey     = false,
         matchValue   = false,
         pairsArray   = Array.new;

    var  indentLocal  = " ".dup(3).join,
         patternLabel  = "",
         str = "";


    //DEFAULTS.
    indent         = indent         ?? "\t";
    initialCallee  = initialCallee  ?? true;


    //
    if (enviroKeys.size > 0, { 
      enviroKeys.do { |key|
        enviroValue = this[key];

        // NB  Match before elision.
        //
        matchKey    = key.asString.regexpMatch(pattern, casei);
        matchValue  = enviroValue.cs.asString.regexpMatch(pattern, casei);

        keyValue = enviroValue.pretty(
                                 elisionLength: elisionLength, 
                                 compileString: compileString,
                                 indent:        indent,
                               );

                                /*
        keyValue = enviroValue.pretty(
                                 pattern:            pattern,
                                 elisionLength:      elisionLength, 

                                 casei:              casei,
                                 compileString:      compileString,
                                 indent:             "\t",
                                 initialCallee:      initialCallee,

                                 title:              title,
                                 minimumSpacerSize:  minimumSpacerSize,
                               );
                                        */

        if (keyValue.isString, { keyValue = keyValue.unquote; });

        if (matchKey || matchValue, {
          pairsArray = pairsArray.addAll([key, keyValue]);
        });

      };
    });


    //
    str = Dump.argsPaired(pairsArray, indent, minimumSpacerSize);

    if (initialCallee, {
      ^("%%").format( 
                //Log.info(thisFunction, title ?? "(environment)"), 
                Log.msg(title ?? "(environment)"), 
                str 
              );
    });

    ^str;

  }  //ment




  //------------------------------------------- -o--
  // MOS protocol methods.

  //--------------------- -o-
  // Pretty print Dictionary-like things...
  //
  pretty  {     | pattern, elisionLength, depth,
                  casei, compileString, indent, initialCallee, 
                  enumerate, bulletIndex, enumerationOffset,
                  minimumSpacerSize, nolog, shortVersion
                |

    var  prefixString  = "\n" ++ (indent ?? ""),
         indentLocal   = " ".dup(3).join,               //XXX
         patternLabel  = "",
         sizeWidthMax  = 65,                            //TBD
         sizeWidth     = 0;

    var  bulletOptions = "*+-.", 
         bulletCharacter,
         thresholdForPostingElementCount     = nil,
         elementCountStr                     = "";

    var  keyList,
         keyStr       = nil,
         keyValue     = "",
         keyValueStr  = "";

    var  str  = "";


    // DEFAULTS.
    //
    depth          = depth          ?? 1;
    initialCallee  = initialCallee  ?? true;
    bulletIndex    = bulletIndex    ?? 0;

    thresholdForPostingElementCount = 
                         thresholdForPostingElementCount ?? 3;   //XXX

    bulletCharacter  = bulletOptions[bulletIndex.mod(bulletOptions.size)];


    // Stopping condition.
    //
    if (depth <= 0, {
      if (this.cs.regexpMatch(pattern, casei).not, { ^""; });
      ^this.csminel(elisionLength, compileString);
    });


    // Process, recurse.
    //
    keyList = this.keys.asList.sort { |x,y|  x.asString < y.asString; };

    keyList.do({ | key
                 |
      if (key.isInteger, {
        keyValue = this[key];
      }, {
        keyValue = this[key.asSymbol];
      });

      if (keyValue.isNil, {
        keyValue     = this[key.asString];
        keyValueStr  = keyValue.asString;

        if (keyValueStr.asString.regexpMatch(pattern, casei), {
          keyValueStr = keyValueStr.minel(elisionLength);
        }, {
          keyValueStr = "";
        });

      }, {
        keyValueStr = keyValue.pretty(    
                                 pattern:         pattern, 
                                 elisionLength:   elisionLength, 
                                   depth:           depth - 1, 

                                 casei:           casei, 
                                 compileString:   compileString, 
                                   indent:          indent ++ indentLocal,
                                   initialCallee:   false, 

                                 enumerate:       enumerate, 
                                   bulletIndex:     bulletIndex + 1,
                               );
      }); 


      //
      keyStr = ("%%% %:  ")
                   .format(prefixString, indentLocal, bulletCharacter, key);

      if ( (keyValueStr.size > 0) 
              || key.asString.regexpMatch(pattern, casei), {

        if (keyValueStr.size <= 0, { 
          keyValueStr = keyValue.csminel(elisionLength, compileString);
        });

        str = str ++ ("%%").format(keyStr, keyValueStr); 
      });

    }); //do


    // Format cases.
    //
    if (initialCallee && pattern.notNil, {
      patternLabel = ("%%\t\t\t\t(pattern: %)\n")
                         .format(prefixString, indentLocal, pattern);
    });

    str = case
      { str.size > 0; }
        { 
          if (this.size >= thresholdForPostingElementCount, {
            elementCountStr = ("-- %").format(this.size);
          });

          ("%(\t\t\t\t%%%%)").format(
                                patternLabel,
                                elementCountStr,
                                str, prefixString, indentLocal 
                              ); 
        }

      { initialCallee; }  { ^patternLabel; }

    ; //endcase

    ^str;

  }  //pretty


} //Dictionary

