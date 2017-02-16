package org.xhtmlrenderer.demo.browser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.xhtmlrenderer.context.StyleReference;
import org.xhtmlrenderer.css.constants.ValueConstants;
import org.xhtmlrenderer.layout.SharedContext;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DOMInspector extends JPanel {
    private static final long serialVersionUID = 1L;

    // PW
    StyleReference styleReference;
    SharedContext context;
    ElementPropertiesPanel elementPropPanel;
    DOMSelectionListener nodeSelectionListener;
    JSplitPane splitPane;
    // PW

    Document doc;
    JButton close;
    JTree tree;

    JScrollPane scroll;

    public DOMInspector(Document doc) {
        this(doc, null, null);
    }

    public DOMInspector(Document doc, SharedContext context, StyleReference sr) {
        super();

        this.setLayout(new java.awt.BorderLayout());

        //JPanel treePanel = new JPanel();
        this.tree = new JTree();
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.scroll = new JScrollPane(tree);

        splitPane = null;
        if (sr == null) {
            add(scroll, "Center");
        } else {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setOneTouchExpandable(true);
            splitPane.setDividerLocation(150);

            this.add(splitPane, "Center");
            splitPane.setLeftComponent(scroll);
        }

        close = new JButton("close");
        this.add(close, "South");
        this.setPreferredSize(new Dimension(300, 300));

        setForDocument(doc, context, sr);

        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getFrame(DOMInspector.this).doDefaultCloseAction();
            }
        });
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawLine(0, 0, 100, 100);
    }

    public void setForDocument(Document doc) {
        setForDocument(doc, null, null);
    }

    public void setForDocument(Document doc, SharedContext context, StyleReference sr) {
        this.doc = doc;
        this.styleReference = sr;
        this.context = context;
        this.initForCurrentDocument();
    }

    public JInternalFrame getFrame(final Component comp) {
        if (comp instanceof JInternalFrame) {
            return (JInternalFrame) comp;
        }
        return getFrame(comp.getParent());
    }

    private void initForCurrentDocument() {
        // tree stuff
        TreeModel model = new DOMTreeModel(doc);
        tree.setModel(model);
        if (!(tree.getCellRenderer() instanceof DOMTreeCellRenderer)) {
            tree.setCellRenderer(new DOMTreeCellRenderer());
        }

        if (styleReference != null) {
            if (elementPropPanel != null) {
                splitPane.remove(elementPropPanel);
            }
            elementPropPanel = new ElementPropertiesPanel(styleReference);
            splitPane.setRightComponent(elementPropPanel);

            tree.removeTreeSelectionListener(nodeSelectionListener);

            //nodeSelectionListener = new DOMSelectionListener( tree, styleReference, elementPropPanel );
            nodeSelectionListener = new DOMSelectionListener(tree, elementPropPanel);
            tree.addTreeSelectionListener(nodeSelectionListener);
        }
    }
}

//-{{{ ElementPropertiesPanel

class ElementPropertiesPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    //private SharedContext _context;
    private StyleReference _sr;
    private JTable _properties;
    private TableModel _defaultTableModel;

    ElementPropertiesPanel(StyleReference sr) {
        super();
        //this._context = context;
        this._sr = sr;

        this._properties = new PropertiesJTable();
        this._defaultTableModel = new DefaultTableModel();

        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(_properties), BorderLayout.CENTER);
    }

    public void setForElement(Node node) {
        try {
            _properties.setModel(tableModel(node));
            TableColumnModel model = _properties.getColumnModel();
            if (model.getColumnCount() > 0) {
                model.getColumn(0).sizeWidthToFit();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private TableModel tableModel(Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            Toolkit.getDefaultToolkit().beep();
            return _defaultTableModel;
        }
        Map props = _sr.getCascadedPropertiesMap((Element) node);
        return new PropertiesTableModel(props);
    }

    static class PropertiesJTable extends JTable {
        private static final long serialVersionUID = 1L;

        Font propLabelFont;
        Font defaultFont;

        PropertiesJTable() {
            super();
            this.setColumnSelectionAllowed(false);
            this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            propLabelFont = new Font("Courier New", Font.BOLD, 12);
            defaultFont = new Font("Default", Font.PLAIN, 12);
        }

        public TableCellRenderer getCellRenderer(int row, int col) {
            JLabel label = (JLabel) super.getCellRenderer(row, col);
            label.setBackground(Color.white);
            label.setFont(defaultFont);
            if (col == 0) {
                // BUG: not working?
                label.setFont(propLabelFont);
            } else if (col == 2) {
                PropertiesTableModel pmodel = (PropertiesTableModel) this.getModel();
                Map.Entry me = (Map.Entry) pmodel._properties.entrySet().toArray()[row];
                CSSPrimitiveValue cpv = (CSSPrimitiveValue) me.getValue();
                if (cpv.getCssText().startsWith("rgb")) {
                    label.setBackground(org.xhtmlrenderer.css.util.ConversionUtil.rgbToColor(cpv.getRGBColorValue()));
                }
            }
            return (TableCellRenderer) label;
        }
    }

    static class PropertiesTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        //String _colNames[] = {"Property Name", "Text", "Value", "Important-Inherit"};
        String _colNames[] = {"Property Name", "Text", "Value"};

        Map _properties;

        PropertiesTableModel(Map cssProperties) {
            _properties = cssProperties;
        }

        public String getColumnName(int col) {
            return _colNames[col];
        }

        public int getColumnCount() {
            return _colNames.length;
        }

        public int getRowCount() {
            return _properties.size();
        }

        public Object getValueAt(int row, int col) {
            Map.Entry me = (Map.Entry) _properties.entrySet().toArray()[row];
            CSSPrimitiveValue cpv = (CSSPrimitiveValue) me.getValue();

            Object val = null;
            switch (col) {

                case 0:
                    val = me.getKey();
                    break;
                case 1:
                    val = cpv.getCssText();
                    break;
                case 2:
                    if (ValueConstants.isNumber(cpv.getPrimitiveType())) {
                        val = new Float(cpv.getFloatValue(cpv.getPrimitiveType()));
                    } else {
                        val = "";//actual.cssValue().getCssText();
                    }
                    break;
                    /* ouch, can't do this now: case 3:
                        val = ( cpv.actual.isImportant() ? "!Imp" : "" ) +
                                " " +
                                ( actual.forcedInherit() ? "Inherit" : "" );
                        break;
                     */
            }
            return val;
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }
}//}}}

//-{{{ DOMSelectionListener

class DOMSelectionListener implements TreeSelectionListener {

    private JTree _tree;
    //private StyleReference _sr;
    private ElementPropertiesPanel _elemPropPanel;

    //DOMSelectionListener( JTree tree, StyleReference sr, ElementPropertiesPanel panel ) {
    DOMSelectionListener(JTree tree, ElementPropertiesPanel panel) {
        _tree = tree;
        //_sr = sr;
        _elemPropPanel = panel;
    }

    public void valueChanged(TreeSelectionEvent e) {
        Node node = (Node) _tree.getLastSelectedPathComponent();

        if (node == null) {
            return;
        }

        _elemPropPanel.setForElement(node);
    }
}//}}}

//-{{{

class DOMTreeModel implements TreeModel {
    Document doc;
    Node root;
    HashMap displayableNodes;
    List listeners = new ArrayList();

    public DOMTreeModel(Document doc) {
        this.displayableNodes = new HashMap();
        this.doc = doc;
        setRoot("body");
    }

    private void setRoot(String rootNodeName) {
        Node tempRoot = doc.getDocumentElement();
        NodeList nl = tempRoot.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeName().toLowerCase().equals(rootNodeName)) {
                this.root = nl.item(i);
            }
        }
    }

    //Adds a listener for the TreeModelEvent posted after the tree changes.

    public void addTreeModelListener(TreeModelListener l) {
        this.listeners.add(l);
    }

    //Removes a listener previously added with addTreeModelListener.

    public void removeTreeModelListener(TreeModelListener l) {
        this.listeners.remove(l);
    }

    //Messaged when the user has altered the value for the item identified by path to newValue.

    public void valueForPathChanged(TreePath path, Object newValue) {
        // no-op
    }

    //Returns the child of parent at index index in the parent's child array.

    public Object getChild(Object parent, int index) {
        Node node = (Node) parent;
        List children = (List) this.displayableNodes.get(parent);
        if (children == null) {
            children = addDisplayable(node);
        }
        return (Node) children.get(index);
    }

    //Returns the number of children of parent.

    public int getChildCount(Object parent) {
        Node node = (Node) parent;
        List children = (List) this.displayableNodes.get(parent);
        if (children == null) {
            children = addDisplayable(node);
        }
        return children.size();
    }

    //Returns the index of child in parent.

    public int getIndexOfChild(Object parent, Object child) {
        Node node = (Node) parent;
        List children = (List) this.displayableNodes.get(parent);
        if (children == null) {
            children = addDisplayable(node);
        }
        if (children.contains(child)) {
            return children.indexOf(child);
        } else {
            return -1;
        }
    }

    //Returns the root of the tree.

    public Object getRoot() {
        return this.root;
    }

    //Returns true if node is a leaf.

    public boolean isLeaf(Object nd) {
        Node node = (Node) nd;
        return !node.hasChildNodes();
    }

    // only adds displayable nodes--not stupid DOM text filler nodes
    private List addDisplayable(Node parent) {
        List children = (List) this.displayableNodes.get(parent);
        if (children == null) {
            children = new ArrayList();
            this.displayableNodes.put(parent, children);
            NodeList nl = parent.getChildNodes();
            for (int i = 0, len = nl.getLength(); i < len; i++) {
                Node child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE ||
                        child.getNodeType() == Node.COMMENT_NODE ||
                        (child.getNodeType() == Node.TEXT_NODE && (child.getNodeValue().trim().length() > 0))) {
                    children.add(child);
                }
            }
            return children;
        } else {
            return new ArrayList();
        }
    }

}//}}}

//-{{{ DOMTreeCellRenderer

class DOMTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 1L;

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        Node node = (Node) value;

        if (node.getNodeType() == Node.ELEMENT_NODE) {

            String cls = "";
            if (node.hasAttributes()) {
                Node cn = node.getAttributes().getNamedItem("class");
                if (cn != null) {
                    cls = " class='" + cn.getNodeValue() + "'";
                }
            }
            value = "<" + node.getNodeName() + cls + ">";

        }

        if (node.getNodeType() == Node.TEXT_NODE) {

            if (node.getNodeValue().trim().length() > 0) {
                value = "\"" + node.getNodeValue() + "\"";
            }
        }

        if (node.getNodeType() == Node.COMMENT_NODE) {

            value = "<!-- " + node.getNodeValue() + " -->";

        }

        DefaultTreeCellRenderer tcr = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        tcr.setOpenIcon(null);
        tcr.setClosedIcon(null);

        return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }
}//}}}
