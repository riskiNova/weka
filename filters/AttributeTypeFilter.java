/*
 *    AttributeTypeFilter.java
 *    Copyright (C) 1999 Intelligenesis Corp.
 *
 */


package weka.filters;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.SparseInstance;
import weka.core.Instances;
import weka.core.OptionHandler;
import java.util.Enumeration;
import weka.core.SelectedTag;
import weka.core.Tag;
import java.util.Vector;
import weka.core.Option;
import weka.core.Utils;
import weka.core.FastVector;

/** 
 * An instance filter that deletes all attributes of a specified type
 * from the dataset.<p>
 *
 * Valid filter-specific options are:<p>
 *
 * -T type <br>
 * Specify the attribute type to delete. Valid values are "nominal", "numeric",
 * and "string". (default "string")<p>
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.3 $
 */
public class AttributeTypeFilter extends Filter implements OptionHandler {

  /** Stores which type of attribute to delete */
  protected int m_DeleteType = Attribute.STRING;

  /* Define possible attribute types to delete */
  public static final Tag [] TAGS_ATTRIBUTES = {
    new Tag(Attribute.STRING, "Delete string attributes"),
    new Tag(Attribute.NOMINAL, "Delete nominal attributes"),
    new Tag(Attribute.NUMERIC, "Delete numeric attributes")
  };

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
              "\tSpecify the attribute type to delete. Valid values are:\n"
	      + "\t\"nominal\", \"numeric\", and \"string\". \n"
              + "(default \"string\")",
              "T", 1, "-T <type>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options controlling the behaviour of this object.
   * Valid options are:<p>
   *
   * -T type <br>
   * Specify the attribute type to delete. Valid values are "nominal", 
   * "numeric", and "string". (default "string")<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String attributeType = Utils.getOption('T', options);
    if (attributeType.length() != 0) {
      attributeType = attributeType.toLowerCase();
      if (attributeType.equals("nominal")) {
        setAttributeType(new SelectedTag(Attribute.NOMINAL, TAGS_ATTRIBUTES));
      } else if (attributeType.equals("numeric")) {
        setAttributeType(new SelectedTag(Attribute.NUMERIC, TAGS_ATTRIBUTES));
      } else {
        setAttributeType(new SelectedTag(Attribute.STRING, TAGS_ATTRIBUTES));
      }
    } else {
      setAttributeType(new SelectedTag(Attribute.STRING, TAGS_ATTRIBUTES));
    }
    
    if (m_InputFormat != null) {
      inputFormat(m_InputFormat);
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [2];
    int current = 0;

    options[current++] = "-T";
    if (m_DeleteType == Attribute.NOMINAL) {
      options[current++] = "nominal";
    } else if (m_DeleteType == Attribute.NUMERIC) {
      options[current++] = "numeric";
    } else {
      options[current++] = "string";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }


  /**
   * Gets the type of attribute that will be deleted. The ID will be one of
   * Attribute.STRING, Attribute.NOMINAL, or Attribute.NUMERIC.
   *
   * @return the selected attribute type.
   */
  public SelectedTag getAttributeType() {

    try {
      return new SelectedTag(m_DeleteType, TAGS_ATTRIBUTES);
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * Sets the type of attribute to delete. Values other than
   * Attribute.STRING, Attribute.NOMINAL, or Attribute.NUMERIC will be ignored.
   *
   * @param newAttributeType the type of attribute to delete.
   */
  public void setAttributeType(SelectedTag newType) {
    
    if (newType.getTags() == TAGS_ATTRIBUTES) {
      m_DeleteType = newType.getSelectedTag().getID();
    }
  }


  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the format couldn't be set successfully
   */
  public boolean inputFormat(Instances instanceInfo) throws Exception {

    m_InputFormat = new Instances(instanceInfo, 0);
    m_NewBatch = true;
    
    // Create the output buffer
    FastVector attributes = new FastVector();
    int outputClass = -1;
    for (int i = 0; i < m_InputFormat.numAttributes(); i++) {
      if (m_InputFormat.attribute(i).type() != m_DeleteType) {
        if (m_InputFormat.classIndex() == i) {
          outputClass = attributes.size();
        }
        attributes.addElement(m_InputFormat.attribute(i));
      }
    }
    Instances outputFormat = new Instances(m_InputFormat.relationName(),
					   attributes, 0); 
    outputFormat.setClassIndex(outputClass);
    setOutputFormat(outputFormat);
    return true;
  }
  

  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception Exception if the input instance was not of the correct 
   * format or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    double[] vals = new double[outputFormatPeek().numAttributes()];
    int j = 0;
    for (int i = 0; i < m_InputFormat.numAttributes(); i++) {
      if (m_InputFormat.attribute(i).type() != m_DeleteType) {
	vals[j++] = instance.value(i);
      }
    }
    if (instance instanceof SparseInstance) {
      push(new SparseInstance(instance.weight(), vals));
    } else {
      push(new Instance(instance.weight(), vals));
    }
    return true;
  }


  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new AttributeTypeFilter(), argv); 
      } else {
	Filter.filterFile(new AttributeTypeFilter(), argv);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
}








