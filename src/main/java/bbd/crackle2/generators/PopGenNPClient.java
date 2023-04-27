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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Vector;

import bbd.crackle2.Action;
import bbd.crackle2.Field;
import bbd.crackle2.Generator;
import bbd.crackle2.Message;
import bbd.crackle2.Module;
import bbd.crackle2.Operation;
import bbd.crackle2.Prototype;
import bbd.crackle2.Table;
import bbd.crackle2.Type;

public class PopGenNPClient extends Generator
{
  public static String description()
  {
    return "Generates Name Pipe Client C DLL and C++ Code";
  }
  public static String documentation()
  {
    return "Generates Name Pipe Client C DLL and C++ Code";
  }
  private static PrintWriter errLog;
  /**
  * Reads input from stored repository
  */
  public static void main(String args[])
  {
    try
    {
      PrintWriter outLog = new PrintWriter(System.out);
      errLog = outLog;
      for (int i = 0; i <args.length; i++)
      {
        outLog.println(args[i]+": Generate ... ");
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(args[i]));
        Module module = (Module)in.readObject();
        in.close();
        generate(module, "", outLog);
      }
      outLog.flush();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  /**
  * Generates
  * - VB Front end interface file
  * - C Header for Server and Client
  * - C Client DLL Marshalling code
  * - C Server Marshalling code
  */
  public static void generate(Module module, String output, PrintWriter outLog)
  {
    errLog = outLog;
    outLog.println(module.name+" version "+module.version);
    generateCClient(module, output, outLog);
    generateCClientHeader(module, output, outLog);
    generateCClientMake(module, output, outLog);
  }
  /**
  * Sets up the writer and generates the general stuff
  */
  public static void generateCClientMake(Module module, String output, PrintWriter outLog)
  {
    try
    {
      File makefile = new File(output+"makefile");
      if (makefile.exists())
        return;
      outLog.println("Generating File: "+output+"makefile");
      OutputStream outFile = new FileOutputStream(output+"makefile");
      PrintWriter outData = new PrintWriter(outFile);
      try
      {
        outData.println(".AUTODEPEND");
        outData.println();
        outData.println("# "+module.name+" makefile");
        outData.println("CC = bcc32");
        outData.println();
        outData.println("INCS = -I..;s:\\bc5\\include");
        outData.println();
        outData.println("LIBS = \\");
        outData.println("\tf:\\jenga\\lib\\libsnap.lib \\");
        outData.println();
        outData.println("CFLAGS = -H- -d -v -y -WD");
        outData.println();
        outData.println(".cpp.obj:");
        outData.println("\t$(CC) -c $(CFLAGS) $(INCS) $<");
        outData.println();
        outData.println(module.name.toUpperCase()+" =\\");
        outData.println("\t"+module.name.toLowerCase()+"Client.obj");
        outData.println();
        outData.println(module.name.toLowerCase()+".dll: $("+module.name.toUpperCase()+")");
        outData.println("\t$(CC) $(CFLAGS) -e"+module.name.toLowerCase()+".dll -Ls:\\bc5\\lib @&&|");
        outData.println("$("+module.name.toUpperCase()+")");
        outData.println("$(LIBS)");
        outData.println("|");
        outData.println();
      }
      finally
      {
        outData.flush();
        outFile.close();
      }
    }
    catch (IOException e1)
    {
      outLog.println("Generate Procs IO Error");
    }
  }
  /**
  * Sets up the writer and generates the general stuff
  */
  public static void generateCClientHeader(Module module, String output, PrintWriter outLog)
  {
    try
    {
      outLog.println("Generating File: "+output+module.name.toLowerCase()+"Client.h");
      OutputStream outFile = new FileOutputStream(output+module.name.toLowerCase()+"Client.h");
      PrintWriter outData = new PrintWriter(outFile);
      try
      {
        outData.println("// This code was generated, do not modify it, modify it at source and regenerate it.");
        outData.println("// Mutilation, Spindlization and Bending will result in ...");
        outData.println("#ifndef _"+module.name+"CLIENT_H_");
        outData.println("#define _"+module.name+"CLIENT_H_");
        outData.println("#pragma option -b");
        outData.println();
        outData.println("#include \"popgen.h\"");
        outData.println();
        outData.println("extern char *"+module.name+"Version;");
        outData.println("extern int32 "+module.name+"Signature;");
        PopGen.generateCExterns(module, outData);
        outData.println("#pragma pack (push, 1)");
        PopGen.generateCStructs(module, outData, true);
        outData.println("#pragma pack (pop)");
        outData.println();
        outData.println("class t"+module.name);
        outData.println("{");
        outData.println("  int32 Handle;");
        outData.println("  e"+module.name+" errorCode;");
        outData.println("  bool loggedOn;");
        outData.println("public:");
        outData.println("  t"+module.name+"() {loggedOn = false;}");
        outData.println("  ~t"+module.name+"() {if (loggedOn) Logoff();}");
        outData.println("  int32 getHandle();");
        outData.println("  void Logon(char* Pipename, int32 Timeout=150000);");
        outData.println("  void Logoff();");
        outData.println("  char* ErrBuffer(char *Buffer, int32 BufferLen);");
        outData.println("  char* ErrorDesc(char *Buffer, int32 BufferLen);");
        for (int i = 0; i < module.prototypes.size(); i++)
        {
          Prototype prototype = (Prototype) module.prototypes.elementAt(i);
          if (prototype.codeType != Prototype.RPCCALL)
            continue;
          PopGen.generateCHeader(module, prototype, outData);
        }
        outData.println("};");
        outData.println();
        outData.println("#endif");
      }
      finally
      {
        outData.flush();
        outFile.close();
      }
    }
    catch (IOException e1)
    {
      outLog.println("Generate Procs IO Error");
    }
  }
  /**
  * Sets up the writer and generates the general stuff
  */
  public static void generateCClient(Module module, String output, PrintWriter outLog)
  {
    try
    {
      String w1 = "";
      outLog.println("Generating File: "+output+module.name.toLowerCase()+"Client.cpp");
      OutputStream outFile = new FileOutputStream(output+module.name.toLowerCase()+"Client.cpp");
      PrintWriter outData = new PrintWriter(outFile);
      try
      {
        outData.println("// This code was generated, do not modify it, modify it at source and regenerate it.");
        outData.println();
        outData.println("#include \"ti.h\"");
        outData.println();
        outData.println("char *"+module.name+"Version = "+module.version+";");
        outData.println("int32 "+module.name+"Signature = "+module.signature+";");
        outData.println();
        outData.println("#include <string.h>");
        outData.println("#include <stdio.h>");
        outData.println("#include <stdlib.h>");
        outData.println();
        outData.println("#include \"popgen.h\"");
        outData.println("#include \"SnapRPCNTClient.h\"");
        outData.println("#include \"handles.h\"");
        outData.println();
        outData.println("#include \""+module.name.toLowerCase()+"Client.h\"");
        outData.println();
        outData.println("char *"+module.name+"Errors[] = ");
        outData.println("{ \"No Error\"");
        for (int i = 0; i < module.messages.size(); i++)
        {
          Message message = (Message) module.messages.elementAt(i);
          outData.println(", "+message.value+"   // "+message.name);
        }
        outData.println(", \"Invalid Signature\"");
        outData.println(", \"Invalid Logon Cookie\"");
        outData.println(", \"Invalid INI File\"");
        outData.println(", \"Uncaught DB Connect Error\"");
        outData.println(", \"Unknown function call\"");
        outData.println(", \"Snap Creation Error\"");
        outData.println(", \"AddList (Index == 0) != (List == 0) Error\"");
        outData.println(", \"AddList (realloc == 0) Error\"");
        outData.println(", \"Last error not in message table\"");
        outData.println("};");
        outData.println();
        for (int i = 0; i < module.tables.size(); i++)
        {
          Table table = (Table) module.tables.elementAt(i);
          String comma = "  ";
          outData.println("char *"+module.name+table.name+"[] = ");
          outData.println("{");
          for (int j = 0; j < table.messages.size(); j++)
          {
            Message message = (Message) table.messages.elementAt(j);
            outData.println(comma+message.value+"   // "+message.name);
            comma = ", ";
          }
          outData.println("};");
          outData.println();
        }
        outData.println("struct tSnapCB");
        outData.println("{");
        outData.println("  tRPCClientNP* RPC;");
        outData.println("  char *sendBuff;");
        outData.println("  int32 sendBuffLen;");
        outData.println("  int32 recvSize;");
        outData.println("  e"+module.name+" result;");
        outData.println("  char errorBuff[4192];");
        outData.println("  int32 RC;");
        Vector<String> retrievals = new Vector<String>();
        Vector<String> submittals = new Vector<String>();
        for (int i = 0; i < module.prototypes.size(); i++)
        {
          Prototype prototype = (Prototype) module.prototypes.elementAt(i);
          if (prototype.codeType != Prototype.RPCCALL)
            continue;
          for (int j = 0; j < prototype.parameters.size(); j++)
          {
            Field parameter = (Field) prototype.parameters.elementAt(j);
            if (parameter.type.typeof != Type.CHAR
            && (parameter.type.reference == Type.BYPTR
            ||  parameter.type.reference == Type.BYREFPTR))
            {
              if (prototype.hasOutputSize(parameter.name)
              ||  prototype.hasInputSize(parameter.name))
              {
                if (parameter.type.reference == Type.BYREFPTR)
                  outData.println("  "+parameter.type.cDefRefPAsP(prototype.name+parameter.name, false)+";");
                else
                  outData.println("  "+parameter.type.cDef(prototype.name+parameter.name, false)+";");
              }
              if (prototype.hasOutputSize(parameter.name))
                retrievals.addElement(prototype.name+parameter.name);
              if (prototype.hasInputSize(parameter.name))
                submittals.addElement(prototype.name+parameter.name);
            }
          }
        }
        outData.println("  tSnapCB(tString Pipename, int32 Timeout)");
        w1 = ":";
        for (int i = 0; i < retrievals.size(); i++)
        {
          String s = (String) retrievals.elementAt(i);
          outData.println("  "+w1+" "+s+"(0)");
          w1 = ",";
        }
        outData.println("  {");
        outData.println("    RPC = new tRPCClientNP(Pipename, Timeout);");
        outData.println("  }");
        outData.println("  ~tSnapCB()");
        outData.println("  {");
        outData.println("    delete RPC;");
        outData.println("  }");
        outData.println("};");
        outData.println();
        outData.println("const CookieStart = 1923;");
        outData.println("const NoCookies = 32;");
        outData.println("static tHandle<tSnapCB*, CookieStart, NoCookies> CBHandle;");
        outData.println();
        outData.println("extern \"C\" e"+module.name+" APPLAPI "+module.name+"SnapLogon(int32* Handle, char* Pipename, int32 Timeout)");
        outData.println("{");
        outData.println("  try");
        outData.println("  {");
        outData.println("    *Handle = CBHandle.Create(new tSnapCB(Pipename, Timeout));");
        outData.println("    return "+module.name.toUpperCase()+"_OK;");
        outData.println("  }");
        outData.println("  catch (xCept &x)");
        outData.println("  {");
        outData.println("    *Handle = -1;");
        outData.println("    return "+module.name.toUpperCase()+"_SNAP_ERROR;");
        outData.println("  }");
        outData.println("}");
        outData.println();
        outData.println("extern \"C\" e"+module.name+" APPLAPI "+module.name+"SnapLogoff(int32* Handle)");
        outData.println("{");
        outData.println("  if (*Handle < CookieStart || *Handle >= CookieStart+NoCookies)");
        outData.println("    return "+module.name.toUpperCase()+"_INV_COOKIE;");
        outData.println("  CBHandle.Release(*Handle);");
        outData.println("  *Handle = -1;");
        outData.println("  return "+module.name.toUpperCase()+"_OK;");
        outData.println("}");
        outData.println();
        outData.println("extern \"C\" e"+module.name+" APPLAPI "+module.name+"SnapErrBuffer(int32 Handle, char *Buffer, int32 BufferLen)");
        outData.println("{");
        outData.println("  if (Handle < CookieStart || Handle >= CookieStart+NoCookies)");
        outData.println("    return "+module.name.toUpperCase()+"_INV_COOKIE;");
        outData.println("  tSnapCB* snapCB = CBHandle.Use(Handle);");
        outData.println("  if (snapCB->RPC->ErrSize())");
        outData.println("    strncpy(Buffer, snapCB->RPC->ErrBuffer(), BufferLen);");
        outData.println("  else");
        outData.println("    strcpy(Buffer, \"\");");
        outData.println("  for(int i = strlen(Buffer); i < BufferLen; i++)");
        outData.println("    Buffer[i]  = ' ';");
        outData.println("  return "+module.name.toUpperCase()+"_OK;");
        outData.println("}");
        outData.println();
        outData.println("extern \"C\" void APPLAPI "+module.name+"SnapErrorDesc(e"+module.name+" RC, char *Buffer, int32 BufferLen)");
        outData.println("{");
        outData.println("  char W1[20]=\"\";");
        outData.println("  if (RC < "+module.name.toUpperCase()+"_OK");
        w1 = "RC";
        if (module.messageBase > 0)
        {
          outData.println("  || (RC > "+module.name.toUpperCase()+"_OK && RC < "+(module.messageBase)+")");
          w1 = "(RC?RC-("+module.messageBase+"-1):0)";
        }
        outData.println("  ||  RC > "+module.name.toUpperCase()+"_LAST_LAST)");
        outData.println("  {");
        outData.println("    sprintf(W1, \" (RC=%d)\", RC);");
        outData.println("    RC = "+module.name.toUpperCase()+"_LAST_LAST;");
        outData.println("  }");
        outData.println("  int n = (int)"+w1+";");
        outData.println("  strncpy(Buffer, "+module.name+"Errors[n], BufferLen-(strlen(W1)+1));");
        outData.println("  if (RC == "+module.name.toUpperCase()+"_LAST_LAST)");
        outData.println("    strcat(Buffer, W1);");
        outData.println("  for(int i = strlen(Buffer); i < BufferLen; i++)");
        outData.println("    Buffer[i]  = ' ';");
        outData.println("}");
        outData.println();
        outData.println("extern \"C\" void APPLAPI "+module.name+"SnapVersion(char *Buffer, int32 BufferLen)");
        outData.println("{");
        outData.println("  strncpy(Buffer, "+module.name+"Version, BufferLen);");
        outData.println("  for(int i = strlen(Buffer); i < BufferLen; i++)");
        outData.println("    Buffer[i]  = ' ';");
        outData.println("}");
        outData.println();
        for (int i = 0; i < module.prototypes.size(); i++)
        {
          Prototype prototype = (Prototype) module.prototypes.elementAt(i);
          if (prototype.codeType != Prototype.RPCCALL)
            continue;
          generateCClient(module, prototype, i, outData);
        }
        outData.println("int32 t"+module.name+"::getHandle()");
        outData.println("{");
        outData.println("  if (Handle < CookieStart || Handle >= CookieStart+NoCookies)");
        outData.println("    throw "+module.name.toUpperCase()+"_INV_COOKIE;");
        outData.println("  return Handle;");
        outData.println("}");
        outData.println();
        outData.println("void t"+module.name+"::Logon(char* Pipename, int32 Timeout)");
        outData.println("{");
        outData.println("  errorCode = "+module.name+"SnapLogon(&Handle, Pipename, Timeout);");
        outData.println("  if (errorCode != 0)");
        outData.println("  {");
        outData.println("    Handle = -1;");
        outData.println("    throw errorCode;");
        outData.println("  }");
        outData.println("  loggedOn = true;");
        outData.println("}");
        outData.println();
        outData.println("void t"+module.name+"::Logoff()");
        outData.println("{");
        outData.println("  loggedOn = false;");
        outData.println("  errorCode = "+module.name+"SnapLogoff(&Handle);");
        outData.println("  if (errorCode != 0)");
        outData.println("  {");
        outData.println("    Handle = -1;");
        outData.println("    throw errorCode;");
        outData.println("  }");
        outData.println("}");
        outData.println();
        outData.println("char* t"+module.name+"::ErrBuffer(char *Buffer, int32 BufferLen)");
        outData.println("{");
        outData.println("  "+module.name+"SnapErrBuffer(getHandle(), Buffer, BufferLen);");
        outData.println("  return Buffer;");
        outData.println("}");
        outData.println();
        outData.println("char* t"+module.name+"::ErrorDesc(char *Buffer, int32 BufferLen)");
        outData.println("{");
        outData.println("  "+module.name+"SnapErrorDesc(errorCode, Buffer, BufferLen);");
        outData.println("  return Buffer;");
        outData.println("}");
        outData.println();
        for (int i = 0; i < module.prototypes.size(); i++)
        {
          Prototype prototype = (Prototype) module.prototypes.elementAt(i);
          if (prototype.codeType != Prototype.RPCCALL)
            continue;
          generateCClientImp(module, prototype, outData);
        }
      }
      finally
      {
        outData.flush();
        outFile.close();
      }
    }
    catch (IOException e1)
    {
      outLog.println("Generate Procs IO Error");
    }
  }
  /**
  * Generates the prototypes defined
  */
  public static void generateCClientImp(Module module, Prototype prototype, PrintWriter outData)
  {
    String w1 = "";
    outData.print(prototype.type.cDef("t"+module.name+"::"+prototype.name, false)+"(");
    for (int i = 0; i < prototype.parameters.size(); i++)
    {
      Field parameter = (Field) prototype.parameters.elementAt(i);
      outData.print(w1 + parameter.type.cDef(parameter.name, false));
      w1 = ", ";
    }
    outData.println(")");
    outData.println("{");
    if (prototype.type.typeof != Type.VOID
    ||  prototype.type.reference == Type.BYPTR)
      outData.println("  "+prototype.type.cName(false)+" Result;");
    outData.print("  errorCode = "+module.name+"Snap" + prototype.name+"(getHandle()");
    outData.print(", "+prototype.signature(false));
    if (prototype.type.typeof != Type.VOID
    ||  prototype.type.reference == Type.BYPTR)
      outData.print(", Result");
    for (int i = 0; i < prototype.parameters.size(); i++)
    {
      Field parameter = (Field) prototype.parameters.elementAt(i);
      outData.print(", "+parameter.name);
    }
    outData.println(");");
    outData.println("  if (errorCode != "+module.name.toUpperCase()+"_OK)");
    outData.println("    throw errorCode;");
    if (prototype.type.typeof != Type.VOID
    ||  prototype.type.reference == Type.BYPTR)
      outData.println("  return Result;");
    outData.println("}");
    outData.println();
  }
  /**
  * Generates the prototypes defined
  */
  public static void generateCClient(Module module, Prototype prototype, int no, PrintWriter outData)
  {
    boolean hasReturn = false;
    Vector<Field> retrievals = new Vector<Field>();
    Vector<Field> submittals = new Vector<Field>();
    if (prototype.type.reference != Type.BYVAL)
    {
      outData.println("#error Only non pointers are allowed as return values");
      errLog.println("#error Only non pointers are allowed as return values");
    }
    if (prototype.type.reference == Type.BYVAL
    &&  prototype.type.typeof != Type.VOID)
      hasReturn = true;
    outData.print("extern \"C\" e"+module.name+" APPLAPI "+module.name+"Snap" + prototype.name+"(int32 Handle, int32 Signature");
    if (hasReturn)
        outData.print(", "+prototype.type.cName(false)+"& Result");
    for (int i = 0; i < prototype.parameters.size(); i++)
    {
      Field parameter = (Field) prototype.parameters.elementAt(i);
      outData.print(", " + parameter.type.cDef(parameter.name, false));
    }
    outData.println(")");
    outData.println("{");
    outData.println("  if (Signature != "+prototype.signature(true)+")");
    outData.println("    return "+module.name.toUpperCase()+"_INV_SIGNATURE;");
    outData.println("  if (Handle < CookieStart || Handle >= CookieStart+NoCookies)");
    outData.println("    return "+module.name.toUpperCase()+"_INV_COOKIE;");
    outData.println("  tSnapCB* snapCB = CBHandle.Use(Handle);");
    outData.println("  try");
    outData.println("  {");
    outData.println("    snapCB->sendBuffLen = 4;");
    for (int i = 0; i < prototype.inputs.size(); i++)
    {
      Action input = (Action) prototype.inputs.elementAt(i);
      Field field  = input.getParameter(prototype);
      if (field == null)
      {
        outData.println("#error "+input.name+" is an undefined input parameter");
        errLog.println("#error "+input.name+" is an undefined input parameter");
        continue;
      }
      if (field.type.reference == Type.BYPTR
      ||  field.type.reference == Type.BYREFPTR)
      {
        if (input.hasSize() == false)
        {
          if (field.type.typeof == Type.CHAR)
            outData.println("    snapCB->sendBuffLen += (sizeof(int32)+strlen("+field.name+")+1);");
          else
            outData.println("    snapCB->sendBuffLen += sizeof(*"+field.name+");");
        }
        else
        {
          Operation op = input.sizeOperation();
          outData.println("    snapCB->sendBuffLen += (sizeof(int32)+"+op.name+"*sizeof(*"+field.name+"));");
        }
      }
      else
      {
        if (input.hasSize() == false)
          outData.println("    snapCB->sendBuffLen += sizeof("+field.name+");");
        else
        {
          outData.println("#error "+field.name+" is not a pointer parameter but has a size");
          errLog.println("#error "+field.name+" is not a pointer parameter but has a size");
        }
      }
    }
    outData.println("    snapCB->sendBuff = new char[snapCB->sendBuffLen];");
    outData.println("    char* ip = snapCB->sendBuff;");
    String w1 = "";
    if (prototype.inputs.size() == 0)
      w1 = "// ";
    outData.println("    *(int32*)ip = Signature;");
    outData.println("    "+w1+"ip += sizeof(int32);");
    for (int i = 0; i < prototype.inputs.size(); i++)
    {
      if (i+1 == prototype.inputs.size())
        w1 = "// ";
      Action input = (Action) prototype.inputs.elementAt(i);
      Field field  = input.getParameter(prototype);
      if (field == null)
        continue;
      Operation op = input.sizeOperation();
      if (field.type.reference == Type.BYPTR
      ||  field.type.reference == Type.BYREFPTR)
      {
        if (input.hasSize() == false)
        {
          if (field.type.typeof == Type.CHAR)
          {
            outData.println("    *(int32*)ip = (strlen("+field.name+")+1);");
            outData.println("    ip += sizeof(int32);");
            outData.println("    memcpy(ip, "+field.name+", (int32)strlen("+field.name+")+1);");
            outData.println("    "+w1+"ip += (strlen("+field.name+")+1);");
          }
          else
          {
            outData.println("    memcpy(ip, (void*)"+field.name+", (int32)sizeof(*"+field.name+"));");
            outData.println("    "+w1+"ip += sizeof(*"+field.name+");");
          }
        }
        else
        {
          submittals.addElement(field);
          outData.println("    *(int32*)ip = ("+op.name+"*sizeof(*"+field.name+"));");
          outData.println("    ip += sizeof(int32);");
          outData.println("    memcpy(ip, (void*)"+field.name+", (int32)("+op.name+"*sizeof(*"+field.name+")));");
          outData.println("    "+w1+"ip += (int32)("+op.name+"*sizeof(*"+field.name+"));");
        }
      }
      else
      {
        outData.println("    memcpy(ip, (char*)&"+field.name+", (int32)sizeof("+field.name+"));");
        outData.println("    "+w1+"ip += sizeof("+field.name+");");
      }
    }
    if (prototype.message.length() > 0)
      w1 = prototype.message;
    else
      w1 = ""+no;
    outData.println("    snapCB->errorBuff[0] = 0;");
    outData.println("    snapCB->RPC->Call("+w1+", snapCB->sendBuff, snapCB->sendBuffLen);");
    outData.println("    delete [] snapCB->sendBuff;");
    outData.println("    snapCB->sendBuff = 0;");
    boolean hasRX     = false;
    w1 = "// ";
    if (prototype.outputs.size() > 0)
      w1 = "";
    if (prototype.outputs.size() > 0 || hasReturn)
    {
      hasRX = true;
      outData.println("    ip = (char*)snapCB->RPC->RxBuffer();");
      if (hasReturn)
      {
        outData.println("    memcpy(&Result, ip, (int32)sizeof(Result));");
        outData.println("    "+w1+"ip += sizeof(Result);");
      }
      for (int i = 0; i < prototype.outputs.size(); i++)
      {
        if (i+1 == prototype.outputs.size())
          w1 = "// ";
        Action output = (Action) prototype.outputs.elementAt(i);
        Field field  = output.getParameter(prototype);
        if (field == null)
        {
          outData.println("#error "+output.name+" is an undefined input parameter");
          errLog.println("#error "+output.name+" is an undefined input parameter");
          continue;
        }
        if (field.type.reference == Type.BYPTR
        ||  field.type.reference == Type.BYREFPTR)
        {
          if (output.hasSize() == false)
          {
            if (field.type.typeof == Type.CHAR)
            {
              outData.println("#error "+output.name+" unsized chars cannot be used as output");
              errLog.println("#error "+output.name+" unsized chars cannot be used as output");
              continue;
            }
            outData.println("    memcpy("+field.name+", ip, sizeof(*"+field.name+"));");
            outData.println("    "+w1+"ip += sizeof(*"+field.name+");");
          }
          else
          {
            retrievals.addElement(field);
            outData.println("    snapCB->recvSize = *(int32*)ip;");
            outData.println("    ip += sizeof(int32);");
            if (field.type.reference == Type.BYREFPTR)
            {
              String s = prototype.getOutputSizeName(field.name);
              Field sf = prototype.getParameter(s);
              String w = "";
              if (sf.type.reference == Type.BYPTR)
                w = "*";
              outData.println("    "+field.name+" = new "+field.type.cName(false)+"["+w+s+"];");
            }
            outData.println("    memcpy("+field.name+", ip, snapCB->recvSize);");
            outData.println("    "+w1+"ip += snapCB->recvSize;");
          }
        }
        else
        {
          outData.println("    memcpy(&"+field.name+", ip, sizeof("+field.name+"));");
          outData.println("    "+w1+"ip += sizeof("+field.name+");");
        }
      }
    }
    if (hasRX)
      outData.println("    snapCB->RPC->RxFree();");
    outData.println("    return (e"+module.name+")snapCB->RPC->ReturnCode();");
    outData.println("  }");
    outData.println("  catch(...)");
    outData.println("  {");
    outData.println("    return "+module.name.toUpperCase()+"_SNAP_ERROR;");
    outData.println("  }");
    outData.println("}");
    outData.println();
    if (submittals.size() > 0)
    {
      for (int i = 0; i < submittals.size(); i++)
      {
        Field parameter = (Field) submittals.elementAt(i);
        outData.println("extern \"C\" void APPLAPI "+module.name+"Snap"+prototype.name+parameter.name+"Prepare(int32 Handle, int32 Size)");
        outData.println("{");
        outData.println("  if (Handle < CookieStart || Handle >= CookieStart+NoCookies)");
        outData.println("    return;");
        outData.println("  tSnapCB* snapCB = CBHandle.Use(Handle);");
        outData.println("  if(snapCB->"+prototype.name+parameter.name+")");
        outData.println("    delete [] snapCB->"+prototype.name+parameter.name+";");
        outData.println("  snapCB->"+prototype.name+parameter.name+" = new "
                            +parameter.type.cName(false)+" [Size];");
        outData.println("}");
        outData.println();
        outData.print("extern \"C\" int32 APPLAPI "+module.name+"Snap"+prototype.name+parameter.name+"Fill(int32 Handle");
        outData.print(", " + parameter.type.cDef("Rec", false));
        outData.println(", int32 Index)");
        outData.println("{");
        outData.println("  if (Handle < CookieStart || Handle >= CookieStart+NoCookies)");
        outData.println("    return 0;");
        outData.println("  tSnapCB* snapCB = CBHandle.Use(Handle);");
        outData.println("  snapCB->"+prototype.name+parameter.name+"[Index] = *Rec;");
        outData.println("  return 1;");
        outData.println("}");
        outData.println();
      }
    }
    if (retrievals.size() > 0 || submittals.size() > 0)
    {
      outData.print("extern \"C\" e"+module.name+" APPLAPI "+module.name+"Snap" + prototype.name + "Start(int32 Handle, int32 Signature");
      if (hasReturn)
        outData.print(", "+prototype.type.cName(false)+"& Result");
      for (int i = 0; i < prototype.parameters.size(); i++)
      {
        Field parameter = (Field) prototype.parameters.elementAt(i);
        if (parameter.type.typeof != Type.CHAR
        && (parameter.type.reference == Type.BYPTR
        ||  parameter.type.reference == Type.BYREFPTR))
          if (prototype.hasOutputSize(parameter.name)
          ||  prototype.hasInputSize(parameter.name))
            continue;
        outData.print(", " + parameter.type.cDef(parameter.name, false));
      }
      outData.println(")");
      outData.println("{");
      outData.println("  if (Handle < CookieStart || Handle >= CookieStart+NoCookies)");
      outData.println("    return "+module.name.toUpperCase()+"_INV_COOKIE;");
      outData.println("  tSnapCB* snapCB = CBHandle.Use(Handle);");
      for (int i = 0; i < retrievals.size(); i++)
      {
        Field parameter = (Field) retrievals.elementAt(i);
        if (parameter.type.reference == Type.BYREFPTR)
          continue;
        String s = prototype.getOutputSizeName(parameter.name);
        Field sf = prototype.getParameter(s);
        String w = "";
        if (sf.type.reference == Type.BYPTR)
          w = "*";
//        outData.println("  if (snapCB->"+prototype.name+parameter.name+" == 0)");
        outData.println("  snapCB->"+prototype.name+parameter.name+" = new "
                        +parameter.type.cName(false)+"["+w+s+"];");
      }
      outData.print("  return "+module.name+"Snap"+prototype.name+"(Handle, Signature");
      if (hasReturn)
        outData.print(", Result");
      for (int i = 0; i < prototype.parameters.size(); i++)
      {
        Field parameter = (Field) prototype.parameters.elementAt(i);
        if (parameter.type.typeof != Type.CHAR
        && (parameter.type.reference == Type.BYPTR
        ||  parameter.type.reference == Type.BYREFPTR)
        && (prototype.hasOutputSize(parameter.name)
        ||  prototype.hasInputSize(parameter.name)))
          outData.print(", snapCB->"+prototype.name+parameter.name);
        else
          outData.print(", "+parameter.name);
      }
      outData.println(");");
      outData.println("}");
      outData.println();
    }
    if (retrievals.size() > 0)
    {
      for (int i = 0; i < retrievals.size(); i++)
      {
        Field parameter = (Field) retrievals.elementAt(i);
        outData.print("extern \"C\" int32 APPLAPI "+module.name+"Snap"+prototype.name+parameter.name+"Next(int32 Handle");
        outData.print(", " + parameter.type.cDefRefPAsP("Rec", false));
        outData.println(", int32 Index)");
        outData.println("{");
        outData.println("  if (Handle < CookieStart || Handle >= CookieStart+NoCookies)");
        outData.println("    return 0;");
        outData.println("  tSnapCB* snapCB = CBHandle.Use(Handle);");
        outData.println("  *Rec = snapCB->"+prototype.name+parameter.name+"[Index];");
        outData.println("  return 1;");
        outData.println("}");
        outData.println();
        outData.println("extern \"C\" void APPLAPI "+module.name+"Snap"+prototype.name+parameter.name+"Done(int32 Handle)");
        outData.println("{");
        outData.println("  if (Handle < CookieStart || Handle >= CookieStart+NoCookies)");
        outData.println("    return;");
        outData.println("  tSnapCB* snapCB = CBHandle.Use(Handle);");
        outData.println("  if(snapCB->"+prototype.name+parameter.name+")");
        outData.println("    delete [] snapCB->"+prototype.name+parameter.name+";");
        outData.println("  snapCB->"+prototype.name+parameter.name+" = 0;");
        outData.println("}");
        outData.println();
      }
    }
  }
}
