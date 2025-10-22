package com.raj.systemdesignsamples.googledocs.gooddesign;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;

//SRP and OCP - are achieved by using Abstraction, Inheritance and polymorphism(overriding).
//OCP - is followed. For new feature we don't need to modify the existing ones.
//LSP - is followed.
//ISP - is followed.
//DIP - is followed.

// Interface for document elements
interface DocumentElement {
    public abstract String render();//SRP - 1. abstraction
}

// Concrete implementation for text elements
class TextElement implements DocumentElement {//SRP - 2 Inheritance
    private String text;

    public TextElement(String text) {
        this.text = text;
    }

    @Override
    public String render() {//SRP - 3. overriding the abstract method.
        return text;
    }
}

// Concrete implementation for image elements
class ImageElement implements DocumentElement {
    private String imagePath;

    public ImageElement(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public String render() {
        return "[Image: " + imagePath + "]";
    }
}

// NewLineElement represents a line break in the document.
class NewLineElement implements DocumentElement {
    @Override
    public String render() {
        return "\n";
    }
}

// TabSpaceElement represents a tab space in the document.
class TabSpaceElement implements DocumentElement {
    @Override
    public String render() {
        return "\t";
    }
}

//It has more than one responsibilities. 1. addElement 2. render.
// So it's better to have separate DocumentRenderer
// then DocumentRenderer can have Document obj as composition.
// Document class responsible for holding a collection of elements
class Document {
    private List<DocumentElement> documentElements = new ArrayList<>();

    public void addElement(DocumentElement element) {//LSP - is followed.
        documentElements.add(element);
    }

    // Renders the document by concatenating the render output of all elements.
    public String render() {
        StringBuilder result = new StringBuilder();
        for (DocumentElement element : documentElements) {
            result.append(element.render());
        }
        return result.toString();
    }
}

// Persistence Interface
interface Persistence {
    void save(String data);
}

// FileStorage implementation of Persistence
class FileStorage implements Persistence {
    @Override
    public void save(String data) {
        try {
            FileWriter outFile = new FileWriter("document.txt");
            outFile.write(data);
            outFile.close();
            System.out.println("Document saved to document.txt");
        } catch (IOException e) {
            System.out.println("Error: Unable to open file for writing.");
        }
    }
}

// Placeholder DBStorage implementation
class DBStorage implements Persistence {
    @Override
    public void save(String data) {
        // Establish connection with DB and Save to DB.
    }
}

//DocumentEditor
// DocumentEditor class managing client interactions
class DocumentEditor {//Has - a
    private Document document;
    private Persistence storage;
    private String renderedDocument = "";

    public DocumentEditor(Document document, Persistence storage) {
        this.document = document;
        this.storage = storage;
    }

    public void addText(String text) {
        document.addElement(new TextElement(text));
    }

    public void addImage(String imagePath) {
        document.addElement(new ImageElement(imagePath));
    }

    // Adds a new line to the document.
    public void addNewLine() {
        document.addElement(new NewLineElement());
    }

    // Adds a tab space to the document.
    public void addTabSpace() {
        document.addElement(new TabSpaceElement());
    }

    public String renderDocument() {//Deligating rendering to
        if (renderedDocument.isEmpty()) {
            renderedDocument = document.render();
        }
        return renderedDocument;
    }

    public void saveDocument() {
        storage.save(renderDocument());
    }
}

// Client usage example
public class DocumentEditorClient {
    public static void main(String[] args) {
        Document document = new Document();
        Persistence persistence = new FileStorage();

        DocumentEditor editor = new DocumentEditor(document, persistence);

        // Simulate a client using the editor with common text formatting features.
        editor.addText("Hello, world!");
        editor.addNewLine();
        editor.addText("This is a real-world document editor example.");
        editor.addNewLine();
        editor.addTabSpace();
        editor.addText("Indented text after a tab space.");
        editor.addNewLine();
        editor.addImage("picture.jpg");

        // Render and display the final document.
        System.out.println(editor.renderDocument());

        editor.saveDocument();
    }
}
