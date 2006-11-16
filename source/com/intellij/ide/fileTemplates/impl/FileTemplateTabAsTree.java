package com.intellij.ide.fileTemplates.impl;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexey Kudravtsev
 */
abstract class FileTemplateTabAsTree extends FileTemplateTab {
  private JTree myTree;
  private FileTemplateNode myRoot;
  private MyTreeModel myTreeModel;

  protected FileTemplateTabAsTree(String title) {
    super(title);
    myRoot = initModel();
    myTreeModel = new MyTreeModel(myRoot);
    myTree = new JTree(myTreeModel);
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    UIUtil.setLineStyleAngled(myTree);

    myTree.expandPath(TreeUtil.getPathFromRoot(myRoot));
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.setCellRenderer(new MyTreeCellRenderer());
    myTree.expandRow(0);

    myTree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        onTemplateSelected();
      }
    });
  }

  protected abstract FileTemplateNode initModel();
  protected static class FileTemplateNode extends DefaultMutableTreeNode {
    private Icon myIcon;
    private final String myTemplate;

    FileTemplateNode(FileTemplateDescriptor descriptor) {
      this(descriptor.getDisplayName(),
           descriptor.getIcon(),
           descriptor instanceof FileTemplateGroupDescriptor ? ContainerUtil.map2List(((FileTemplateGroupDescriptor)descriptor).getTemplates(), new Function<FileTemplateDescriptor, FileTemplateNode>() {
             public FileTemplateNode fun(FileTemplateDescriptor s) {
               return new FileTemplateNode(s);
             }
           }) : Collections.<FileTemplateNode>emptyList(),
           descriptor instanceof FileTemplateGroupDescriptor ? null : descriptor.getFileName());
    }

    FileTemplateNode(String name, Icon icon, List<FileTemplateNode> children) {
      this(name, icon, children, null);
    }

    FileTemplateNode(Icon icon, String template) {
      this(template, icon, Collections.<FileTemplateNode>emptyList(), template);
    }

    private FileTemplateNode(String name, Icon icon, List<FileTemplateNode> children, String template) {
      super(name);
      myIcon = icon;
      myTemplate = template;
      for (FileTemplateNode child : children) {
        add(child);
      }
    }

    public Icon getIcon() {
      return myIcon;
    }

    public String getTemplate() {
      return myTemplate;
    }

  }

  private static class MyTreeModel extends DefaultTreeModel {
    MyTreeModel(FileTemplateNode root) {
      super(root);
    }
  }

  private class MyTreeCellRenderer extends DefaultTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

      if (value instanceof FileTemplateNode) {
        final FileTemplateNode node = (FileTemplateNode)value;
        setText((String) node.getUserObject());
        setIcon(node.getIcon());
        setFont(getFont().deriveFont(AllFileTemplatesConfigurable.isInternalTemplate(node.getTemplate(), getTitle()) ? Font.BOLD : Font.PLAIN));
      }
      return this;
    }
  }

  public void removeSelected() {
    // not supported
  }

  protected void initSelection(FileTemplate selection) {
    if (selection != null) {
      selectTemplate(selection);
    }
    else {
      TreeUtil.selectFirstNode(myTree);
    }
  }

  public void selectTemplate(FileTemplate template) {
    String name = template.getName();
    if (template.getExtension() != null && template.getExtension().length() > 0) {
      name += "." + template.getExtension();
    }
    
    final FileTemplateNode node = (FileTemplateNode)TreeUtil.findNodeWithObject(myRoot, name);
    if (node != null) {
      TreeUtil.selectNode(myTree, node);
      onTemplateSelected(); // this is important because we select different Template for the same node
    }
  }

  @Nullable
  public FileTemplate getSelectedTemplate() {
    final TreePath selectionPath = myTree.getSelectionPath();
    if (selectionPath == null) return null;
    final FileTemplateNode node = (FileTemplateNode)selectionPath.getLastPathComponent();
    final String template = node.getTemplate();
    if (template == null) return null;
    return savedTemplates.get(FileTemplateManager.getInstance().getJ2eeTemplate(template));
  }

  public JComponent getComponent() {
    return myTree;
  }

  public void fireDataChanged() {
  }

  public void addTemplate(FileTemplate newTemplate) {
    // not supported
  }
}
