package nodecluster;

import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.io.IndentingXMLStreamWriter;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public interface ActionFormatter  {
	
        void startOutput() throws Exception;

        void endOutput() throws Exception;

        void startMatches() throws Exception;

        void match(ITree srcNode, ITree destNode) throws Exception;

        void endMatches() throws Exception;

        void startActions() throws Exception;

        void insertRoot(ITree node) throws Exception;

        void insertAction(ITree node, ITree parent, int index) throws Exception;

        void moveAction(ITree src, ITree dst, int index) throws Exception;

        void updateAction(ITree src, ITree dst) throws Exception;

        void deleteAction(ITree node) throws Exception;

        void endActions() throws Exception;
    }
