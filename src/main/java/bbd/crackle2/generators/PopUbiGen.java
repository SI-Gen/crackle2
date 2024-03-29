/// ------------------------------------------------------------------
/// Copyright (c) from 1996 Vincent Risi
/// 
/// All rights reserved.
/// This program and the accompanying materials are made available
/// under the terms of the Common Public License v1.0
/// which accompanies this distribution and is available at
/// http://www.eclipse.org/legal/cpl-v10.html
/// Contributors:
///    Vincent Risi
/// ------------------------------------------------------------------
package bbd.crackle2.generators;

import java.io.PrintWriter;

import bbd.crackle2.Action;
import bbd.crackle2.Enumerator;
import bbd.crackle2.Field;
import bbd.crackle2.Message;
import bbd.crackle2.Module;
import bbd.crackle2.Operation;
import bbd.crackle2.Prototype;
import bbd.crackle2.Structure;
import bbd.crackle2.Table;
import bbd.crackle2.Type;

public class PopUbiGen
{
  public static boolean qualifyEnums=false;
  public static PrintWriter errLog = new PrintWriter(System.out);
  /**
  * Sets up the writer and generates the general stuff
  */
  public static void generateCExterns(Module module, PrintWriter outData)
  {
    boolean done = false;
    for (int i = 0; i < module.structures.size(); i++)
    {
      boolean found = false;
      Structure struct1 = (Structure) module.structures.elementAt(i);
      if (struct1.codeType != Structure.NORMAL)
        continue;
      if (struct1.header.length() > 0)
      {
        for (int j = 0; j < i; j++)
        {
          Structure struct2 = (Structure) module.structures.elementAt(j);
          if (struct2.header.compareTo(struct1.header) == 0)
          {
            found = true;
            break;
          }
        }
        if (found == false)
        {
          outData.println("#include " + struct1.header);
          done = true;
        }
      }
    }
    if (done)
      outData.println();
  }
  /**
  * Generates the data structures defined
  */
  public static void generateCStructs(Module module, PrintWriter outData, boolean doSwap)
  {
    generateCStructs(module, outData, doSwap, true);
  }
  /**
  * Generates the data structures defined
  */
  public static void generateCStructs(Module module, PrintWriter outData, boolean doSwap, boolean allMessages)
  {
    outData.println("enum e"+module.name);
    outData.println("{ "+module.name.toUpperCase()+"_OK");
    String w0 = "";
    if (module.messageBase > 0)
      w0 = " = "+module.messageBase;
    for (int i = 0; i < module.messages.size(); i++)
    {
      Message message = (Message) module.messages.elementAt(i);
      outData.println(", "+module.name.toUpperCase()+"_"+message.name+w0+" // "+message.value);
      w0 = "";
    }
    outData.println(", "+module.name.toUpperCase()+"_INV_SIGNATURE");
    outData.println(", "+module.name.toUpperCase()+"_INV_COOKIE");
    if (allMessages)
    {
      outData.println(", "+module.name.toUpperCase()+"_INV_INIFILE");
      outData.println(", "+module.name.toUpperCase()+"_UNCAUGHT_DB_ERROR");
      outData.println(", "+module.name.toUpperCase()+"_UNKNOWN_FUNCTION");
      outData.println(", "+module.name.toUpperCase()+"_SNAP_ERROR");
      outData.println(", "+module.name.toUpperCase()+"_ADDLIST_ERROR");
      outData.println(", "+module.name.toUpperCase()+"_ADDLIST_REALLOC_FAILED");
    }
    outData.println(", "+module.name.toUpperCase()+"_LAST_LAST");
    outData.println("};");
    outData.println();
    outData.println("extern char *"+module.name+"Errors[];");
    outData.println();
    for (int i = 0; i < module.tables.size(); i++)
    {
      Table table = (Table) module.tables.elementAt(i);
      outData.println("enum e"+module.name+table.name);
      outData.println("{");
      String comma = "  ";
      for (int j = 0; j < table.messages.size(); j++)
      {
        Message message = (Message) table.messages.elementAt(j);
        outData.println(comma+table.name.toLowerCase()+message.name+"   // "+message.value);
        comma = ", ";
      }
      outData.println(comma+table.name.toLowerCase()+"NoOf");
      outData.println("};");
      outData.println();
      outData.println("extern char *"+module.name+table.name+"[];");
      outData.println();
    }
    boolean qualify = module.enumerators.size() > 1 ? true : qualifyEnums;
    if (module.enumerators.size() > 0)
    {
      for (int i = 0; i < module.enumerators.size(); i++)
      {
        Enumerator entry = (Enumerator) module.enumerators.elementAt(i);
        String qualifier = "";
        if (qualify == true)
          qualifier = entry.name + "_";
        outData.println("enum " + entry.name);
        String w1 = "{ ";
        for (int j = 0; j < entry.elements.size(); j++)
        {
          String element = (String)entry.elements.elementAt(j);
          outData.println(w1 + qualifier + element);
          w1 = ", ";
        }
        outData.println("};");
        outData.println();
      }
    }
    for (int i = 0; i < module.structures.size(); i++)
    {
      Structure structure = (Structure) module.structures.elementAt(i);
      if (structure.codeType != Structure.NORMAL)
        continue;
      if (structure.fields.size() > 0)
      {
        outData.println("struct "+structure.name);
        outData.println("{");
        int relativeOffset = 0;
        int fillerNo = 0;
        for (int j=0; j < structure.fields.size(); j++)
        {
          Field field = (Field) structure.fields.elementAt(j);
          int n = (relativeOffset+8) % 8;// field.type.paddingSize(false);
          if (n > 0)
          {
            n = 8-n;
            outData.println("  char filler"+fillerNo+"["+n+"];");
            fillerNo++;
            relativeOffset += n;
          }
          outData.println("  "+field.type.cDefUbi(field.name)+";");
          relativeOffset += field.type.relativeSize(false);
        }
        int n = (relativeOffset+8) % 8;
        if (n > 0)
        {
          n = 8-n;
          outData.println("  char filler"+fillerNo+"["+n+"];");
        }
        outData.println("  "+structure.name+"()");
        outData.println("  {");
        for (int k = 0; k < structure.fields.size(); k++)
        {
          Field field = (Field)structure.fields.elementAt(k);
          if (field.type.typeof == Type.CHAR)
            outData.println("    " + field.name + "[0] = 0;");
          else if (field.type.typeof == Type.USERTYPE)
            ;
          else if (field.type.arraySizes.size() == 1)
            outData.println("    " + field.name + "[0] = 0;");
          else
            outData.println("    " + field.name + " = 0;");
        }
        outData.println("  }");
        if (doSwap)
        {
          outData.println("  void Swaps()");
          outData.println("  {");
          for (int k = 0; k < structure.fields.size(); k++)
          {
            String w1 = "", w2 = "", w3 = ".", w4 = "", w5 = "";
            Field field = (Field)structure.fields.elementAt(k);
            if (field.type.reference == Type.BYPTR
                || field.type.reference == Type.BYREFPTR)
            {
              w3 = "->";
              w4 = "*";
            }
            if (field.needsSwap()
                || field.isStruct(module))
            {
              for (int j = 0; j < field.type.arraySizes.size(); j++)
              {
                Integer integer = (Integer)field.type.arraySizes.elementAt(j);
                outData.println("    " + w2 + "{for (int k" + j + " = 0; k" + j
                                + " < " + integer.intValue()
                                + "; k" + j + "++)");
                w1 = w1 + "[k" + j + "]";
                w2 = w2 + "  ";
                w5 = w5 + "}";
              }
            }
            if (field.needsSwap())
              outData.println("    " + w2 + "SwapBytes(" + w4 + field.name + w1 + ");" + w5);
            else if (field.isStruct(module))
              outData.println("    " + w2 + field.name + w1 + w3 + "Swaps();" + w5);
          }
          outData.println("  }");
          outData.println("  #if defined(_DATABUILD_H_)");
          outData.println("  static int NoBuildFields() {return " + structure.fields.size() + ";}");
          outData.println("  void _buildAdds(DataBuilder &dBuild)");
          outData.println("  {");
          for (int k = 0; k < structure.fields.size(); k++)
          {
            Field field = (Field)structure.fields.elementAt(k);
            if (field.type.typeof == Type.BYTE)
            {
              if (field.type.arraySizes.size() == 1)
              {
                Integer integer = (Integer)field.type.arraySizes.elementAt(0);
                outData.println("    dBuild.addByte(\"" + field.name + "\", " + field.name + ", " + integer.intValue() + ");");
              }
              else
                outData.println("    dBuild.addByte(\"" + field.name + "\", " + field.name + ");");
            }
            else
              outData.println("    dBuild.add(\"" + field.name + "\", " + field.name + ");");
          }
          outData.println("  }");
          outData.println("  void BuildData(DataBuilder &dBuild, const char* name=\"" + structure.name + "\")");
          outData.println("  {");
          outData.println("    dBuild.name(name);");
          outData.println("    _buildAdds(dBuild);");
          outData.println("  }");
          outData.println("  void _buildSets(DataBuilder &dBuild)");
          outData.println("  {");
          for (int k = 0; k < structure.fields.size(); k++)
          {
            Field field = (Field)structure.fields.elementAt(k);
            if (field.type.typeof == Type.BYTE)
            {
              if (field.type.arraySizes.size() == 1)
              {
                Integer integer = (Integer)field.type.arraySizes.elementAt(0);
                outData.println("    dBuild.setByte(\"" + field.name + "\", " + field.name + ", " + integer.intValue() + ");");
              }
              else
                outData.println("    dBuild.setByte(\"" + field.name + "\", " + field.name + ", sizeof(" + field.name + "));");
            }
            else
              outData.println("    dBuild.set(\"" + field.name + "\", " + field.name + ", sizeof(" + field.name + "));");
          }
          outData.println("  }");
          outData.println("  void SetData(DataBuilder &dBuild, const char* name=\"" + structure.name + "\")");
          outData.println("  {");
          outData.println("    dBuild.name(name);");
          outData.println("    _buildSets(dBuild);");
          outData.println("  }");
          outData.println("  #endif");
        }
        outData.println("  template <class T> void _copyFrom(T& src)");
        outData.println("  {");
        for (int k = 0; k < structure.fields.size(); k++)
        {
          Field field = (Field)structure.fields.elementAt(k);
          if (field.type.typeof == Type.CHAR)
          {
            outData.println("    strncpy(" + field.name + ", src." + field.name + ", sizeof(" + field.name + ")-1);");
            outData.println("    " + field.name + "[sizeof(" + field.name + ")-1] = 0;");
          }
          else if (field.type.typeof == Type.USERTYPE)
            outData.println("    memcpy(" + field.name + ", src." + field.name + ", sizeof(" + field.name + "));");
          else
            outData.println("    " + field.name + " = src." + field.name + ";");
        }
        outData.println("  }");
        outData.println("  template <class T> void _copyTo(T& src)");
        outData.println("  {");
        for (int k = 0; k < structure.fields.size(); k++)
        {
          Field field = (Field)structure.fields.elementAt(k);
          if (field.type.typeof == Type.CHAR)
          {
            outData.println("    strncpy(src." + field.name + ", " + field.name + ", sizeof(src." + field.name + ")-1);");
            outData.println("    src." + field.name + "[sizeof(src." + field.name + ")-1] = 0;");
          }
          else if (field.type.typeof == Type.USERTYPE)
            outData.println("    memcpy(src." + field.name + ", " + field.name + ", sizeof(src." + field.name + "));");
          else
            outData.println("    src." + field.name + " = " + field.name + ";");
        }
        outData.println("  }");
        outData.println("};");
        outData.println();
      }
    }
  }
  /**
  * Generates the actions
  */
  private static void generateCActions(Module module, Prototype prototype, PrintWriter outData, String pad)
  {
    outData.println(pad+"/**");
    if (prototype.inputs.size() > 0)
    {
      outData.println(pad+"* INPUT:");
      for (int i = 0; i < prototype.inputs.size(); i++)
      {
        Action input = (Action) prototype.inputs.elementAt(i);
        Operation op = input.sizeOperation();
        outData.println(pad+"*   "+input.name+((op!=null)?" "+op.name:"")+";");
      }
    }
    if (prototype.outputs.size() > 0)
    {
      outData.println(pad+"* OUTPUT:");
      for (int i = 0; i < prototype.outputs.size(); i++)
      {
        Action output = (Action) prototype.outputs.elementAt(i);
        Operation op = output.sizeOperation();
        outData.println(pad+"*   "+output.name+((op!=null)?" "+op.name:"")+";");
      }
    }
    outData.println(pad+"* RETURNS: "+prototype.type.cNameUbi());
    outData.println(pad+"*/");
  }
  /**
  * Generates the prototypes defined
  */
  public static void generateCHeader(Module module, Prototype prototype, PrintWriter outData)
  {
    generateCActions(module, prototype, outData, "  ");
    String w1 = "";
    outData.print("  "+prototype.type.cDefUbi(prototype.name)+"(");
    for (int i = 0; i < prototype.parameters.size(); i++)
    {
      Field parameter = (Field) prototype.parameters.elementAt(i);
      outData.print(w1 + parameter.type.cDefUbi(parameter.name));
      w1 = ", ";
    }
    outData.println(");");
  }
  /**
  * Generates the prototypes defined
  */
  public static void generateCInterface(Module module, Prototype prototype, PrintWriter outData)
  {
    generateCActions(module, prototype, outData, "  ");
    String w1 = "";
    outData.print("  virtual " + prototype.type.cDefUbi(prototype.name) + "(");
    for (int i = 0; i < prototype.parameters.size(); i++)
    {
      Field parameter = (Field)prototype.parameters.elementAt(i);
      outData.print(w1 + parameter.type.cDefUbi(parameter.name));
      w1 = ", ";
    }
    outData.println(") = 0;");
  }
  /**
  * Generates the prototypes defined
  */
  public static void generateVirtualCHeader(Module module, Prototype prototype, PrintWriter outData)
  {
    generateCActions(module, prototype, outData, "  ");
    String w1 = "";
    outData.print("  virtual " + prototype.type.cDefUbi(prototype.name) + "(");
    for (int i = 0; i < prototype.parameters.size(); i++)
    {
      Field parameter = (Field)prototype.parameters.elementAt(i);
      outData.print(w1 + parameter.type.cDefUbi(parameter.name));
      w1 = ", ";
    }
    outData.println(") = 0;");
  }
  /**
  * Generates the implementation code
  */
  public static void generateCImplCode(Module module, Prototype prototype, PrintWriter outData)
  {
    generateCActions(module, prototype, outData, "");
    String w1 = "";
    outData.print(prototype.type.cDefUbi("t"+module.name+"::"+prototype.name)+"(");
    for (int i = 0; i < prototype.parameters.size(); i++)
    {
      Field parameter = (Field) prototype.parameters.elementAt(i);
      outData.print(w1 + parameter.type.cDefUbi(parameter.name));
      w1 = ", ";
    }
    outData.println(")");
  }
  /**
  * Generates the implementation code
  */
  public static void generateUbiCImplCode(Module module, Prototype prototype, PrintWriter outData)
  {
    generateCActions(module, prototype, outData, "");
    String w1 = "";
    outData.print(prototype.type.cDefUbi("T" + module.name + "::" + prototype.name) + "(");
    for (int i = 0; i < prototype.parameters.size(); i++)
    {
      Field parameter = (Field)prototype.parameters.elementAt(i);
      outData.print(w1 + parameter.type.cDefUbi(parameter.name));
      w1 = ", ";
    }
    outData.println(")");
  }
  /**
  * Generates the prototypes defined
  */
  public static void generateCSwaps(Module module, Prototype prototype, Field field, Operation op, PrintWriter outData)
  {
    String w1 = "", w2 = "", w3 = ".", w4 = "", w5 = "", w6 = "";
    int n=0;
    if (field.needsSwap()
        ||  field.isStruct(module))
    {
      if (field.type.reference == Type.BYPTR
          ||  field.type.reference == Type.BYREFPTR)
      {
        w3 = "->";
        w4 = "*";
      }
      if (op != null)
      {
        Field opField = prototype.getParameter(op.name);
        if (opField != null
            && (opField.type.reference == Type.BYPTR
                ||  opField.type.reference == Type.BYREFPTR))
          w5 = "*";
        n++;
        outData.println("    "+w2+"{for (int _i_"+n+op.name+" = 0; _i_"+n+op.name+" < "+w5+op.name+"; _i_"+n+op.name+"++)");
        w1 = w1 + "[_i_"+n+op.name+"]";
        w2 = w2 + "  ";
        if (field.type.reference == Type.BYPTR)
          w3 = ".";
        w4 = "";
        w6 = w6+"}";
      }
      for (int j=0; j < field.type.arraySizes.size(); j++)
      {
        Integer integer = (Integer) field.type.arraySizes.elementAt(j);
        n++;
        outData.println("    "+w2+"{for (int i_"+n+j+ " = 0; i_"+n+j
                        + " < " + integer.intValue()
                        + "; i_"+n+j+"++)");
        w1 = w1 + "[i_"+n+j+"]";
        w2 = w2 + "  ";
        w3 = ".";
        w4 = "";
        w6 = w6+"}";
      }
    }
    if (field.isStruct(module))
      outData.println("    "+w2+field.name+w1+w3+"Swaps();"+w6);
    else if (field.needsSwap())
      outData.println("    "+w2+"SwapBytes("+w4+field.name+w1+");"+w6);
    else if (field.type.typeof == Type.USERTYPE)
    {
      //Toolkit.getDefaultToolkit().beep();
      errLog.println("Warning: "+prototype.name+" "+field.name+" is of UserType and may require swapping.");
    }
  }
  /**
  * Generates the prototypes defined
  */
  public static void generateCTrims(Module module, Prototype prototype, Field field, Operation op, PrintWriter outData)
  {
    String w1 = "", w2 = "", w3 = ".", w4 = "", w5 = "", w6 = "";
    int n=0;
    if (field.needsTrim() == true
    ||  field.isStruct(module))
    {
      if (field.type.reference == Type.BYPTR
      ||  field.type.reference == Type.BYREFPTR)
      {
        w3 = "->";
        w4 = "*";
      }
      if (op != null)
      {
        Field opField = prototype.getParameter(op.name);
        if (opField != null
            && (opField.type.reference == Type.BYPTR
                ||  opField.type.reference == Type.BYREFPTR))
          w5 = "*";
        n++;
        outData.println("    "+w2+"{for (int _i_"+n+op.name+" = 0; _i_"+n+op.name+" < "+w5+op.name+"; _i_"+n+op.name+"++)");
        w1 = w1 + "[_i_"+n+op.name+"]";
        w2 = w2 + "  ";
        if (field.type.reference == Type.BYPTR)
          w3 = ".";
        w4 = "";
        w6 = w6+"}";
      }
      for (int j=1; j < field.type.arraySizes.size(); j++)
      {
        Integer integer = (Integer) field.type.arraySizes.elementAt(j);
        n++;
        outData.println("    "+w2+"{for (int i_"+n+j+ " = 0; i_"+n+j
                        + " < " + integer.intValue()
                        + "; i_"+n+j+"++)");
        w1 = w1 + "[i_"+n+j+"]";
        w2 = w2 + "  ";
        w3 = ".";
        w4 = "";
        w6 = w6+"}";
      }
    }
    if (field.isStruct(module))
      outData.println("    "+w2+field.name+w1+w3+"Trims();"+w6);
    else if (field.needsTrim())
      outData.println("    "+w2+"TrimTrailingBlanks("+w4+field.name+w1+", sizeof("+w4+field.name+w1+"));"+w6);
    else if (field.type.typeof == Type.USERTYPE)
    {
      //Toolkit.getDefaultToolkit().beep();
      errLog.println("Warning: "+prototype.name+" "+field.name+" is of UserType and may require swapping.");
    }
  }
  public static void generateCStructSwaps(Module module, Prototype prototype, Field field, Operation op, PrintWriter outData)
  {
    if (field.isStruct(module) == true)
    {
      generateCSwaps(module, prototype, field, op, outData);
      generateCTrims(module, prototype, field, op, outData);
    }
  }
  public static void generateCNonStructSwaps(Module module, Prototype prototype, Field field, Operation op, PrintWriter outData)
  {
    if (field.isStruct(module) == false)
    {
      generateCSwaps(module, prototype, field, op, outData);
      generateCTrims(module, prototype, field, op, outData);
    }
  }
}
