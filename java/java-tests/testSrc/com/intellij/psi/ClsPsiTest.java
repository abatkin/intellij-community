/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi;

import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.IoTestUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.compiled.ClsParameterImpl;
import com.intellij.psi.impl.java.stubs.PsiMethodStub;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import com.intellij.testFramework.LightIdeaTestCase;
import com.intellij.util.indexing.FileBasedIndex;

import java.io.File;
import java.io.IOException;

public class ClsPsiTest extends LightIdeaTestCase {
  private static final String TEST_DATA_PATH = "/psi/cls/repo";

  private PsiClass myObjectClass;
  private GlobalSearchScope myScope;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(getModule());
    myObjectClass = getJavaFacade().findClass(CommonClassNames.JAVA_LANG_OBJECT, myScope);
    assertNotNull(myObjectClass);
  }

  public void testClassFileUpdate() throws IOException {
    File testFile = IoTestUtil.createTestFile("TestClass.class");
    File file1 = new File(PathManagerEx.getTestDataPath() + TEST_DATA_PATH + "/1_TestClass.class");
    FileUtil.copy(file1, testFile);
    VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(testFile);
    assertNotNull(testFile.getPath(), vFile);
    FileBasedIndex.getInstance().requestReindex(vFile);
    PsiFile file = PsiManager.getInstance(getProject()).findFile(vFile);
    assertNotNull(file);

    PsiClass aClass = ((PsiJavaFile)file).getClasses()[0];
    assertTrue(aClass.isValid());
    assertEquals(2, aClass.getFields().length);
    assertEquals("field11", aClass.getFields()[0].getName());
    assertEquals("field12", aClass.getFields()[1].getName());
    assertEquals(2, aClass.getMethods().length);
    assertEquals("TestClass", aClass.getMethods()[0].getName());
    assertEquals("method1", aClass.getMethods()[1].getName());

    File file2 = new File(PathManagerEx.getTestDataPath() + TEST_DATA_PATH + "/2_TestClass.class");
    FileUtil.copy(file2, testFile);
    assertTrue(testFile.setLastModified(System.currentTimeMillis() + 5000));
    vFile.refresh(false, false);

    aClass = ((PsiJavaFile)file).getClasses()[0];
    assertTrue(aClass.isValid());
    assertTrue(aClass.isValid());
    assertEquals(1, aClass.getFields().length);
    assertEquals("field2", aClass.getFields()[0].getName());
    assertEquals(2, aClass.getMethods().length);
    assertEquals("TestClass", aClass.getMethods()[0].getName());
    assertEquals("method2", aClass.getMethods()[1].getName());
  }

  public void testFile() {
    PsiJavaFile file = getFile("MyClass");
    assertTrue(file.isValid());
    assertEquals("pack", file.getPackageName());
    assertEquals(1, file.getClasses().length);
  }

  public void testClassBasics() {
    PsiJavaFile file = getFile("MyClass");
    PsiClass aClass = file.getClasses()[0];
    assertTrue(aClass.isValid());

    assertEquals(file, aClass.getParent());
    assertEquals(file, aClass.getContainingFile());
    assertEquals("MyClass", aClass.getName());
    assertEquals("pack.MyClass", aClass.getQualifiedName());
    assertFalse(aClass.isInterface());
    assertFalse(aClass.isDeprecated());
  }

  public void testClassMembers() {
    PsiClass aClass = getFile("MyClass").getClasses()[0];

    PsiClass[] inners = aClass.getInnerClasses();
    assertEquals(1, inners.length);
    assertEquals(aClass, inners[0].getParent());

    PsiField[] fields = aClass.getFields();
    assertEquals(2, fields.length);
    assertEquals(aClass, fields[0].getParent());

    PsiMethod[] methods = aClass.getMethods();
    assertEquals(3, methods.length);
    assertEquals(aClass, methods[0].getParent());

    assertNotNull(aClass.findFieldByName("field1", false));
  }

  public void testModifierList() {
    PsiClass aClass = getFile("MyClass").getClasses()[0];

    PsiModifierList modifierList = aClass.getModifierList();
    assertNotNull(modifierList);
    assertTrue(modifierList.hasModifierProperty(PsiModifier.PUBLIC));
    assertFalse(modifierList.hasModifierProperty(PsiModifier.STATIC));
    assertEquals(modifierList.getParent(), aClass);

    PsiField field = aClass.getFields()[0];
    modifierList = field.getModifierList();
    assertNotNull(modifierList);
    assertTrue(modifierList.hasModifierProperty(PsiModifier.PACKAGE_LOCAL));
    assertEquals(modifierList.getParent(), field);

    PsiMethod method = aClass.getMethods()[0];
    modifierList = method.getModifierList();
    assertNotNull(modifierList);
    assertTrue(modifierList.hasModifierProperty(PsiModifier.PACKAGE_LOCAL));
    assertEquals(modifierList.getParent(), method);
  }

  public void testField() {
    PsiClass aClass = getFile("MyClass").getClasses()[0];
    PsiField field1 = aClass.getFields()[0], field2 = aClass.getFields()[1];

    assertEquals("field1", field1.getName());

    PsiType type1 = field1.getType();
    assertEquals(PsiType.INT, type1);
    assertEquals("int", type1.getPresentableText());
    assertTrue(type1 instanceof PsiPrimitiveType);

    PsiType type2 = field2.getType();
    assertTrue(type2.equalsToText("java.lang.Object[]"));
    assertEquals("Object[]", type2.getPresentableText());
    assertFalse(type2 instanceof PsiPrimitiveType);
    assertTrue(type2 instanceof PsiArrayType);

    PsiType componentType = ((PsiArrayType)type2).getComponentType();
    assertTrue(componentType.equalsToText(CommonClassNames.JAVA_LANG_OBJECT));
    assertEquals(CommonClassNames.JAVA_LANG_OBJECT_SHORT, componentType.getPresentableText());
    assertFalse(componentType instanceof PsiPrimitiveType);
    assertFalse(componentType instanceof PsiArrayType);

    PsiLiteralExpression initializer = (PsiLiteralExpression)field1.getInitializer();
    assertNotNull(initializer);
    assertEquals("123", initializer.getText());
    assertEquals(123, initializer.getValue());
    assertEquals(PsiType.INT, initializer.getType());

    assertFalse(field2.hasInitializer());
    assertNull(field2.getInitializer());
  }

  public void testMethod() {
    PsiClass aClass = getFile("MyClass").getClasses()[0];

    assertEquals("method1", aClass.getMethods()[0].getName());

    PsiMethod method1 = aClass.getMethods()[0];
    PsiTypeElement type1 = method1.getReturnTypeElement();
    assertNotNull(type1);
    assertEquals(method1, type1.getParent());
    assertEquals("void", type1.getText());
    assertTrue(type1.getType() instanceof PsiPrimitiveType);
    assertFalse(method1.isConstructor());

    PsiMethod method3 = aClass.getMethods()[2];
    assertNull(method3.getReturnType());
    assertTrue(method3.isConstructor());
  }

  public void testTypeReference() {
    PsiClass aClass = getFile("MyClass").getClasses()[0];

    PsiType type1 = aClass.getFields()[0].getType();
    assertNull(PsiUtil.resolveClassInType(type1));

    PsiType type2 = aClass.getFields()[1].getType();
    PsiType componentType = ((PsiArrayType)type2).getComponentType();
    assertTrue(componentType instanceof PsiClassType);
    assertTrue(componentType.equalsToText(CommonClassNames.JAVA_LANG_OBJECT));
    assertEquals(CommonClassNames.JAVA_LANG_OBJECT_SHORT, componentType.getPresentableText());

    PsiElement target = PsiUtil.resolveClassInType(type2);
    assertEquals(myObjectClass, target);
  }

  public void testReferenceLists() {
    PsiClass aClass = getFile("MyClass").getClasses()[0];

    PsiReferenceList extList = aClass.getExtendsList();
    assertNotNull(extList);
    PsiClassType[] refs = extList.getReferencedTypes();
    assertEquals(1, refs.length);
    assertEquals("ArrayList", refs[0].getPresentableText());
    assertEquals("java.util.ArrayList", refs[0].getCanonicalText());

    PsiReferenceList implList = aClass.getImplementsList();
    assertNotNull(implList);
    PsiClassType[] implRefs = implList.getReferencedTypes();
    assertEquals(1, implRefs.length);
    assertEquals("Cloneable", implRefs[0].getPresentableText());
    assertEquals("java.lang.Cloneable", implRefs[0].getCanonicalText());

    PsiReferenceList throwsList = aClass.getMethods()[0].getThrowsList();
    PsiClassType[] throwsRefs = throwsList.getReferencedTypes();
    assertEquals(2, throwsRefs.length);
    assertEquals("Exception", throwsRefs[0].getPresentableText());
    assertEquals("java.lang.Exception", throwsRefs[0].getCanonicalText());
    assertEquals("IOException", throwsRefs[1].getPresentableText());
    assertEquals("java.io.IOException", throwsRefs[1].getCanonicalText());
  }

  public void testParameters() {
    PsiClass aClass = getFile("MyClass").getClasses()[0];

    PsiParameter[] parameters = aClass.getMethods()[0].getParameterList().getParameters();
    assertEquals(2, parameters.length);

    PsiType type1 = parameters[0].getType();
    assertEquals("int[]", type1.getPresentableText());
    assertTrue(type1.equalsToText("int[]"));
    assertTrue(type1 instanceof PsiArrayType);
    assertNull(PsiUtil.resolveClassInType(type1));

    PsiType type2 = parameters[1].getType();
    assertEquals("Object", type2.getPresentableText());
    assertTrue(type2.equalsToText(CommonClassNames.JAVA_LANG_OBJECT));
    assertFalse(type2 instanceof PsiArrayType);
    assertFalse(type2 instanceof PsiPrimitiveType);
    PsiClass target2 = PsiUtil.resolveClassInType(type2);
    assertEquals(myObjectClass, target2);

    assertNotNull(parameters[0].getModifierList());
    assertNotNull(parameters[1].getModifierList());

    assertTrue(((ClsParameterImpl)parameters[0]).isAutoGeneratedName());
    assertTrue(((ClsParameterImpl)parameters[1]).isAutoGeneratedName());
    assertEquals("ints", parameters[0].getName());
    assertEquals("o", parameters[1].getName());
  }

  @SuppressWarnings("ConstantConditions")
  public void testModifiers() {
    PsiClass aClass = getFile().getClasses()[0];
    assertEquals("private transient", aClass.getFields()[0].getModifierList().getText());
    assertEquals("private volatile", aClass.getFields()[1].getModifierList().getText());
    assertEquals("public", aClass.getMethods()[0].getModifierList().getText());
    assertEquals("private", aClass.getMethods()[1].getModifierList().getText());
    assertEquals("private synchronized", aClass.getMethods()[2].getModifierList().getText());
  }

  public void testEnum() {
    PsiClass aClass = getFile("MyEnum").getClasses()[0];
    assertTrue(aClass.isEnum());
    PsiField[] fields = aClass.getFields();
    PsiClassType type = getJavaFacade().getElementFactory().createType(aClass);
    assertEnumConstant(fields[0], "RED", type);
    assertEnumConstant(fields[1], "GREEN", type);
    assertEnumConstant(fields[2], "BLUE", type);
  }

  private static void assertEnumConstant(PsiField field, String name, PsiClassType type) {
    assertEquals(name, field.getName());
    assertTrue(field instanceof PsiEnumConstant);
    assertEquals(type, field.getType());
  }

  public void testAnnotations() {
    PsiClass aClass = getFile("Annotated").getClasses()[0];

    PsiModifierList modifierList = aClass.getModifierList();
    assertNotNull(modifierList);
    PsiAnnotation[] annotations = modifierList.getAnnotations();
    assertEquals(1, annotations.length);
    assertTrue(annotations[0].getText().equals("@pack.Annotation"));

    PsiMethod method = aClass.getMethods()[1];
    annotations = method.getModifierList().getAnnotations();
    assertEquals(1, annotations.length);
    assertTrue(annotations[0].getText().equals("@pack.Annotation"));

    PsiParameter[] params = method.getParameterList().getParameters();
    assertEquals(1, params.length);
    modifierList = params[0].getModifierList();
    assertNotNull(modifierList);
    annotations = modifierList.getAnnotations();
    assertEquals(1, annotations.length);
    assertTrue(annotations[0].getText().equals("@pack.Annotation"));

    modifierList = aClass.getFields()[0].getModifierList();
    assertNotNull(modifierList);
    annotations = modifierList.getAnnotations();
    assertEquals(1, annotations.length);
    assertTrue(annotations[0].getText().equals("@pack.Annotation"));
  }

  public void testAnnotationMethods() {
    PsiMethod[] methods = getFile("Annotation").getClasses()[0].getMethods();
    assertNotNull(((PsiAnnotationMethod)methods[0]).getDefaultValue());

    methods = getFile("Annotation2").getClasses()[0].getMethods();
    for (PsiMethod method : methods) {
      assertTrue(String.valueOf(method), method instanceof PsiAnnotationMethod);
      try {
        PsiAnnotationMemberValue defaultValue = ((PsiAnnotationMethod)method).getDefaultValue();
        assertTrue(String.valueOf(defaultValue), defaultValue instanceof PsiBinaryExpression);
        PsiPrimitiveType type = method.getName().startsWith("f") ? PsiType.FLOAT : PsiType.DOUBLE;
        assertEquals(type, ((PsiBinaryExpression)defaultValue).getType());
      }
      catch (Exception e) {
        String valueText = ((PsiMethodStub)((StubBasedPsiElement)method).getStub()).getDefaultValueText();
        fail("Unable to compute default value of method " + method + " from text '" + valueText + "': " + e.getMessage());
      }
    }
  }

  public void testKeywordAnnotatedClass() {
    PsiModifierList modList = getFile("../KeywordAnnotatedClass").getClasses()[0].getModifierList();
    assertNotNull(modList);
    assertEquals("@pkg.native", modList.getAnnotations()[0].getText());
  }

  public void testGenerics() {
    PsiClass aClass = getJavaFacade().findClass(CommonClassNames.JAVA_UTIL_HASH_MAP, myScope);
    assertNotNull(aClass);

    PsiTypeParameter[] parameters = aClass.getTypeParameters();
    assertEquals(2, parameters.length);
    assertEquals("K", parameters[0].getName());
    assertEquals("V", parameters[1].getName());

    PsiClassType extType = aClass.getExtendsListTypes()[0];
    assertEquals("java.util.AbstractMap<K,V>", extType.getCanonicalText());

    PsiType returnType = aClass.findMethodsByName("entrySet", false)[0].getReturnType();
    assertNotNull(returnType);
    assertEquals("java.util.Set<java.util.Map.Entry<K,V>>", returnType.getCanonicalText());

    PsiType paramType = aClass.findMethodsByName("putAll", false)[0].getParameterList().getParameters()[0].getType();
    assertNotNull(paramType);
    assertEquals("java.util.Map<? extends K,? extends V>", paramType.getCanonicalText());
    assertNotNull(((PsiClassType)paramType).resolveGenerics().getElement());
  }

  public void testFqnCorrectness() {
    PsiClass aClass = getFile("$Weird$Name").getClasses()[0].getInnerClasses()[0];
    assertEquals("pack.$Weird$Name.Inner", aClass.getQualifiedName());
  }

  private PsiJavaFile getFile() {
    return getFile(getTestName(false));
  }

  private static PsiJavaFile getFile(String name) {
    String path = PathManagerEx.getTestDataPath() + TEST_DATA_PATH + "/pack/" + name + ".class";
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    assertNotNull(path, file);
    PsiFile clsFile = PsiManager.getInstance(getProject()).findFile(file);
    assertTrue(String.valueOf(clsFile), clsFile instanceof ClsFileImpl);
    return (PsiJavaFile)clsFile;
  }
}