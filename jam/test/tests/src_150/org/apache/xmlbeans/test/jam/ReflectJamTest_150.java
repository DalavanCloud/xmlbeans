/*
* The Apache Software License, Version 1.1
*
*
* Copyright (c) 2003 The Apache Software Foundation.  All rights
* reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by the
*        Apache Software Foundation (http://www.apache.org/)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Apache" and "Apache Software Foundation" must
*    not be used to endorse or promote products derived from this
*    software without prior written permission. For written
*    permission, please contact apache@apache.org.
*
* 5. Products derived from this software may not be called "Apache
*    XMLBeans", nor may "Apache" appear in their name, without prior
*    written permission of the Apache Software Foundation.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*
* This software consists of voluntary contributions made by many
* individuals on behalf of the Apache Software Foundation and was
* originally based on software copyright (c) 2003 BEA Systems
* Inc., <http://www.bea.com/>. For more information on the Apache Software
* Foundation, please see <http://www.apache.org/>.
*/
package org.apache.xmlbeans.test.jam;

import org.apache.xmlbeans.impl.jam.JamServiceFactory;
import org.apache.xmlbeans.impl.jam.JamServiceParams;
import org.apache.xmlbeans.impl.jam.JamService;
import org.apache.xmlbeans.impl.jam.JamClassLoader;
import org.apache.xmlbeans.impl.jam.JClass;
import org.apache.xmlbeans.impl.jam.internal.reflect.ReflectClassBuilder;
import org.apache.xmlbeans.impl.jam.internal.java15.Reflect15DelegateImpl;

import java.io.IOException;
import java.io.File;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Runs the JamTestBase cases by loading the types from source.
 *
 * @author Patrick Calahan &lt;email: pcal-at-bea-dot-com&gt;
 */
public class ReflectJamTest_150 extends JamTestBase_150 {

  // ========================================================================
  // Constructors

  public ReflectJamTest_150(String name) {
    super(name);
  }

  // ========================================================================
  // JamTestBase implementation


  protected JamService getResultToTest() throws IOException {
    JamServiceFactory jsf = JamServiceFactory.getInstance();
    JamServiceParams params = jsf.createServiceParams();
//params.setVerbose(ReflectClassBuilder.class);
//params.setVerbose(Reflect15DelegateImpl.class);
    params.includeClassPattern(getCasesClassPath(),"**/*.class");
    return jsf.createService(params);
  }

  protected boolean isAnnotationsAvailable() { return false; }

  protected boolean isImportsAvailable() { return false; }


  //kind of a quick hack for now, should remove this and make sure that
  //even the classes case make the annotations available using a special
  //JStore
  protected boolean is175AnnotationInstanceAvailable() {
    return true;
  }

  protected boolean isParameterNamesKnown() { return false; }

  protected boolean isCommentsAvailable() {
    return false;
  }

  protected File getMasterDir() { return new File(TEST_DIR,"masters/reflect"); }

  // ========================================================================
  // Reflection-specific test methods

  // ========================================================================
  // Reflection-specific test methods

//FIXME please move this up into base class

  public void testClassLoaderWrapper() throws MalformedURLException {
    File aJarNotInTheClasspath = EXTJAR_JAR;
    assertTrue(aJarNotInTheClasspath.getAbsolutePath()+" does not exist",
               aJarNotInTheClasspath.exists());
    URL url = aJarNotInTheClasspath.toURL();
    ClassLoader cl = new URLClassLoader(new URL[] {url},
                                        ClassLoader.getSystemClassLoader());
    JamClassLoader jcl = JamServiceFactory.getInstance().createJamClassLoader(cl);
    String aClassName = "foo.SomeClassInAnExternalJar";
    JClass aClass = jcl.loadClass(aClassName);
    assertTrue(aClass.getQualifiedName(),!aClass.isUnresolvedType());
    //sanity check it now
    JamClassLoader sjcl = JamServiceFactory.getInstance().createSystemJamClassLoader();
    JClass aFailedClass = sjcl.loadClass(aClassName);
    assertTrue(aFailedClass.getQualifiedName()+" expected to be unresolved",
               aFailedClass.isUnresolvedType());

  }
}
