/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

package com.intellij.ide.actions;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.WriteActionAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The base class for actions which create new file elements.
 */
public abstract class CreateElementActionBase extends CreateInDirectoryActionBase implements WriteActionAware {

  protected CreateElementActionBase() {
  }

  protected CreateElementActionBase(@NlsActions.ActionText String text,
                                    @NlsActions.ActionDescription String description,
                                    Icon icon) {
    super(text, description, icon);
  }

  protected CreateElementActionBase(Supplier<String> dynamicText, Supplier<String> dynamicDescription, Icon icon) {
    super(dynamicText, dynamicDescription, icon);
  }


  /**
   * @return created elements. Never null.
   * @deprecated use async variant
   * {@link CreateElementActionBase#invokeDialog(Project, PsiDirectory, Consumer)} instead
   */
  @Deprecated
  protected PsiElement @NotNull [] invokeDialog(Project project, PsiDirectory directory) {
    return PsiElement.EMPTY_ARRAY;
  }

  /**
   * Overloaded version of {@link CreateElementActionBase#invokeDialog(Project, PsiDirectory)}
   * adapted for asynchronous calls
   * @param elementsConsumer describes actions with created elements
   */
  protected void invokeDialog(@NotNull Project project, @NotNull PsiDirectory directory, @NotNull Consumer<PsiElement[]> elementsConsumer) {
    elementsConsumer.accept(invokeDialog(project, directory));
  }

  /**
   * @return created elements. Never null.
   */
  protected abstract PsiElement @NotNull [] create(@NotNull String newName, PsiDirectory directory) throws Exception;

  @Nls(capitalization = Nls.Capitalization.Title)
  protected abstract String getErrorTitle();

  /**
   * @deprecated this method isn't called by the platform; {@link #getActionName(PsiDirectory, String)} is used instead.
   */
  @Deprecated
  protected String getCommandName() {
    return "";
  }

  @Nls
  protected abstract String getActionName(PsiDirectory directory, String newName);

  @Override
  public final void actionPerformed(@NotNull final AnActionEvent e) {
    final IdeView view = getIdeView(e);
    if (view == null) {
      return;
    }

    final Project project = e.getProject();

    final PsiDirectory dir = view.getOrChooseDirectory();
    if (dir == null || project == null) return;
    invokeDialog(project, dir, createdElements -> {
      for (PsiElement createdElement : createdElements) {
        view.selectElement(createdElement);
      }
    });
  }

  @Nullable
  protected IdeView getIdeView(@NotNull AnActionEvent e) {
    return e.getData(LangDataKeys.IDE_VIEW);
  }

  public static String filterMessage(String message) {
    if (message == null) return null;
    @NonNls final String ioExceptionPrefix = "java.io.IOException:";
    message = StringUtil.trimStart(message, ioExceptionPrefix);
    return message;
  }

  protected class MyInputValidator extends ElementCreator implements InputValidator {
    private final PsiDirectory myDirectory;
    private PsiElement[] myCreatedElements = PsiElement.EMPTY_ARRAY;

    public MyInputValidator(final Project project, final PsiDirectory directory) {
      super(project, getErrorTitle());
      myDirectory = directory;
    }

    public PsiDirectory getDirectory() {
      return myDirectory;
    }

    @Override
    public boolean checkInput(final String inputString) {
      return true;
    }

    @Override
    public PsiElement[] create(@NotNull String newName) throws Exception {
      return CreateElementActionBase.this.create(newName, myDirectory);
    }

    @Override
    public boolean startInWriteAction() {
      return CreateElementActionBase.this.startInWriteAction();
    }

    @Override
    public String getActionName(String newName) {
      return CreateElementActionBase.this.getActionName(myDirectory, newName);
    }

    @Override
    public boolean canClose(final String inputString) {
      myCreatedElements = tryCreate(inputString);
      return myCreatedElements.length > 0;
    }

    public final PsiElement[] getCreatedElements() {
      return myCreatedElements;
    }
  }
}
