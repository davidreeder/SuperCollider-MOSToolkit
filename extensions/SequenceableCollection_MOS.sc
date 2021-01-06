//
// SequenceableCollection_MOS.sc
//
//
// MOS PROTOCOL SUPPORT--
//   *mosVersion
//   pretty
//
//
// MOS DEPENDENCIES--
//   Object_MOS
//   String_MOS
//
//
//---------------------------------------------------------------------
//     Copyright (C) David Reeder 2018-2020.  sc@mobilesound.org
//     Distributed under the Boost Software License, Version 1.0.
//     (See ./LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
//---------------------------------------------------------------------


+ SequenceableCollection  {

  //------------------------------------------- -o--
  *mosVersion  { 
    var  extensionVersion  = "0.1";   //RELEASE
    ^this.mosVersionString(extensionVersion);
  }



  //------------------------------------------- -o--
  // Instance methods.

  //--------------------- -o-
  // Pretty print array-like things...
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

    var  lineNumber  = "",
         digitMax    = (this.size - 1).asString.size,
         elemStr,
         thresholdForPostingElementCount     = nil,
         elementCountStr                     = "";

    var  str  = "";


    //DEFAULTS.
    //
    depth          = depth          ?? 1;
    initialCallee  = initialCallee  ?? true;
    enumerate      = enumerate      ?? true;

    thresholdForPostingElementCount = 
                         thresholdForPostingElementCount ?? 3;   //XXX


    // Stopping condition.
    //
    if (depth <= 0, {
      if (this.cs.regexpMatch(pattern, casei).not, { ^""; });
      ^this.csminel(elisionLength, compileString);
    });


    // Process, recurse.
    //
    this.do { |elem, index|
      if (enumerate, { 
        lineNumber = ("%:  ").format(index.asString.padLeft(digitMax, " ")); 
      });

      elemStr = elem.pretty( 
                       pattern:          pattern, 
                       elisionLength:    elisionLength, 
                         depth:          depth - 1, 
 
                       casei:            casei, 
                       compileString:    compileString, 
                         indent:         indent ++ indentLocal,
                         initialCallee:  false, 

                       enumerate:        enumerate, 
                       bulletIndex:      bulletIndex,
                     );

      if (elemStr.size > 0, {
        str = str ++ ("%%%%").format(
                                prefixString, indentLocal, lineNumber, elemStr);
      });
    };


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


} //SequenceableCollection

