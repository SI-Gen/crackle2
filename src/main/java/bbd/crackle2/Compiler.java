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
package bbd.crackle2;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Vector;

import bbd.crackle2.parsers.IDL;

public class Compiler
{
  /**
   * Reads input from stored repository
   */
  static PrintWriter outLog;
  static String preprocessFlags = "";
  static String idlFileName = "";
  static String iiDirName = "";
  static String ibDirName = "";
  static boolean preprocess = false;
  static boolean combine = false;
  static Vector<Integer> messageNos;
  private static boolean mkPath(String pathname)
  {
    File path = new File(pathname);
    boolean result = path.exists();
    if (result == false)
      result = path.mkdirs();
    return result;
  }
  private static boolean mkFilesPath(String filename)
  {
    File check = new File(filename);
    if (check.getParent() == null)
      return true;
    boolean result = mkPath(check.getParent());
    if (result == false)  
      System.out.println("Directory of " + filename + " did not make.");
    return result;
  }
  private static String[] argsParse(String args[]) throws FileNotFoundException
  {
    boolean reported = false;
    int i = 0, no = args.length;
    while (i < no)
    {
      String arg = args[i];
      if (arg.charAt(0) != '-')
        break;
      if (arg.length() == 1)
      {
        System.out.println("A singular '-' is an invalid command line argument");
        i++;
        break;
      }
      if (arg.charAt(1) == '-')
        break;
      switch (arg.charAt(1))
      {
      case 'b':
        if (arg.length() > 2)
          ibDirName = arg.substring(2);
        else if (i < no && args[i+1].charAt(0) != '-')
        {
          i++;
          ibDirName = args[i];
        }
        else
          ibDirName = "";
        combine = true;
        break;
      case 'i':
        if (arg.length() > 2)
          iiDirName = arg.substring(2);
        else if (i < no && args[i+1].charAt(0) != '-')
        {
          i++;
          iiDirName = args[i];
        }
        else
          iiDirName = "";
        combine = true;
        break;
      case 'l':
        String logName;
        if (arg.length() > 2)
          logName = arg.substring(2);
        else if (i < no)
        {
          i++;
          logName = args[i];
        } 
        else
          break;
//        if (mkFilesPath(logName) == true)
//        {
//          System.out.println("Switch Logging to " + logName);
//          OutputStream outFile = new FileOutputStream(logName);
//          outLog = new PrintWriter(outFile);
//        }
        break;
      case 'f':
        if (arg.length() > 2)
          idlFileName = arg.substring(2);
        else if (i < no)
        {
          i++;
          idlFileName = args[i];
        }
        mkFilesPath(idlFileName);
        break;
      case 'p':
        if (arg.length() > 2)
          preprocessFlags = arg.substring(2);
        else if (i < no)
        {
          String check = args[i+1];
          if (check.charAt(0) != '-')
          {
            i++;
            preprocessFlags = args[i];
          }
        }
        preprocess = true;
        break;
      default:
        System.out.println("'" + arg + "' is an invalid command line argument");
        if (reported == false)
        {
          reported = true;
          System.out.println("Valid command line arguments here are");
          System.out.println(" -b <dir>    Directory containing source with .ib extension for business logic");
          System.out.println(" -f <name>   Filename of .idl file to be combined");
          System.out.println(" -i <dir>    Directory containing source with .ii extension from db .si files");
          System.out.println(" -l <name>   Change logging name");
          System.out.println(" -p <string> Preprocess flags");
        }
        break;
      }
      i++;
    }
    if (i == 0)
      return args;
    String[] result = new String[no - i];
    System.arraycopy(args, i, result, 0, no - i);
    return result;
  }

  public static void main(String args[])
  {
    outLog = new PrintWriter(System.out);//, true);
    System.out.println("Starting Crackle compile");
    System.out.println("Args: [" + Arrays.toString(args) + "]");
    try
    {
      args = argsParse(args);
      Module module;
      if (combine == true)
      {
        System.out.println("Combining");
        messageNos = new Vector<Integer>();
        String code = combineFileReader(args[0]);
        if (idlFileName.length() > 0)
        {
          FileWriter fileWriter = new FileWriter(idlFileName);
          PrintWriter writer = new PrintWriter(fileWriter);
          writer.println(code);
          writer.flush();
          writer.close();
          fileWriter.close();
        }
        StringReader reader = new StringReader(code);
        module = IDL.run(args[0], reader, outLog);
      }
      else if (preprocess == true)
      {
        System.out.println("Prepocessing");
        messageNos = new Vector<Integer>();
        String code = processFileReader(args[0]);
        StringReader reader = new StringReader(code);
        module = IDL.run(args[0], reader, outLog);
      } else
      {
        System.out.println("Compiling file " + args[0]);
        module = IDL.run(args[0], outLog);
        System.out.println("Compile completed of " + args[0]);
      }
      if (module == null)
      {
        System.out.println("Error: Compile aborted due to errors");
        outLog.flush();
        System.exit(1);
      }
      String output = "";
      for (int i = 1; i < args.length; i++)
      {
        if (args[i].equals("-o"))
        {
          if (i + 1 < args.length)
          {
            output = args[++i];
            char term = '\\';
            if (output.indexOf('/') != -1)
              term = '/';
            char ch = output.charAt(output.length() - 1);
            if (ch != term)
              output = output + term;
            mkPath(output);
            System.out.println("Generating to " + output);
          }
          continue;
        } else if (args[i].equals("-l"))
        {
//          if (i + 1 < args.length)
//          {
//            String log = args[++i];
//            mkFilesPath(log);
//            OutputStream outFile = new FileOutputStream(log);
//            outLog.flush();
//            outLog = new PrintWriter(outFile);
//          }
          continue;
        }
        String curGenerator = args[i];
        System.out.println("Calling generator " + args[i]);
        Class<?> c = Class.forName("bbd.crackle2.generators." + curGenerator);
        Class<?> d[] = { module.getClass(), output.getClass(), outLog.getClass() };
        Method m = c.getMethod("generate", d);
        Object o[] = { module, output, outLog };
        m.invoke(null, o);
        System.out.println("Finished " + curGenerator + " generator.");
      }
      System.out.println("Crackle Generation completed.");
      System.exit(0);
    } catch (Exception e)
    {
      System.out.println("Error: " + e + "[" + e.getMessage() + "]");
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("Jportal call completed");
  }
  private static int uniqueNumber(Vector<Integer> messageNos, int procNumber)
  {
    if (procNumber < 0)
      procNumber *= -1;
    incProcNumber: for (;; procNumber++)
    {
      for (int i = 0; i < messageNos.size(); i++)
      {
        Integer n = (Integer) messageNos.elementAt(i);
        if (n.intValue() == procNumber)
          continue incProcNumber;
      }
      messageNos.addElement(Integer.valueOf(procNumber));
      break;
    }
    return procNumber;
  }

  /*
   * with a do preprocess flag in the form below - I use the colons as a
   * convention the :xxx: is tested to see if it is contained in the string
   * -p:drop:keep:
   * 
   * //$ifnot :drop: 
   *   code 
   *   code 
   * //$endif 
   * ... 
   * //$if :keep: 
   *   code 
   *   code 
   * //$endif
   */
  static String processFileReader(String fileName)
  {
    StringBuilder builder = new StringBuilder();
    try
    {
      System.out.println(fileName + " to be read");
      FileReader fileReader = new FileReader(fileName);
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      try
      {
        boolean insertLine = true;
        while (bufferedReader.ready())
        {
          String line = bufferedReader.readLine();
          String w = line;
          if (w.indexOf("//$include") == 0)
          {
            String newFileName = w.substring(10).trim();
            builder.append(processFileReader(newFileName));
            continue;
          }
          if (w.indexOf("//$message") == 0)
          {
            builder.append("message:"
                + uniqueNumber(messageNos, w.substring(10).trim().hashCode())
                + ";");
            continue;
          }
          if (w.indexOf("//$if") == 0)
          {
            boolean sense = true;
            w = w.substring(5).trim();
            if (w.indexOf("not") == 0)
            {
              sense = false;
              w = w.substring(3).trim();
            }
            if (preprocessFlags.indexOf(w) != -1)
              insertLine = sense;
            else
              insertLine = !sense;
            System.out.println((insertLine == true ? "keeping " : "dropping ") + w);
            continue;
          }
          if (w.indexOf("//$endif") == 0)
          {
            insertLine = true;
            continue;
          }
          if (insertLine == true)
          {
            builder.append(line);
            builder.append('\n');
          }
        }
        bufferedReader.close();
      } catch (NullPointerException e2)
      {
      }
    } catch (FileNotFoundException e)
    {
      System.out.println("PreProcess FileNotFoundException");
    } catch (IOException e)
    {
      System.out.println("PreProcess IOException");
    }
    return builder.toString();
  }
  private static String combineFileReader(String fileName)
  {
    StringBuilder builder = new StringBuilder();
    builder.append(processFileReader(fileName));
    appendFromDir(builder, iiDirName, ".ii");
    appendFromDir(builder, ibDirName, ".ib");
    return builder.toString();
  }
  private static String fullName(String dirName, String fileName)
  {
    String dirsep = "/";
    if (dirName.length() > 2 && dirName.charAt(1) == ':')
      dirsep = "\\";
    String result = dirName + dirsep + fileName;
    return result;
  }
  private static void appendFromDir(StringBuilder builder, String dirName, String ext)
  {
    if (dirName.length() == 0)
      return;
    File dir = new File(dirName);
    String[] list = dir.list();
    if (list == null)
    {
      System.out.println(dirName + " does not appear to be a good file directory");
    }
    else
    {
      for (int i = 0; i < list.length; i++)
      {
        String name = list[i];
        int n = name.length();
        if (n <= 3)
          continue;
        if (name.substring(n-3).compareTo(ext) == 0)
        {
          String combName = fullName(dirName, name);
          builder.append("// -- " + combName);
          builder.append('\n');
          builder.append(processFileReader(combName));
        }
      }
    }
  }
}
