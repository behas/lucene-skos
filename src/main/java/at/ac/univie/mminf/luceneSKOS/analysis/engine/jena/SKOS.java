package at.ac.univie.mminf.luceneSKOS.analysis.engine.jena;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Vocabulary definitions from skos.rdf
 */
public interface SKOS {
    /**
     * The ontology model that holds the vocabulary terms
     */
    OntModel m_model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, null );
    
    /**
     * The namespace of the vocabulary as a string
     */
    String NS = "http://www.w3.org/2004/02/skos/core#";

    /**
     * The namespace of the vocabulary as a resource
     */
    Resource NAMESPACE = m_model.createResource( NS );
    
    ObjectProperty broadMatch = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#broadMatch" );
    
    /**
     * Broader concepts are typically rendered as parents in a concept hierarchy(tree).
     */
    ObjectProperty broader = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#broader" );
    
    ObjectProperty broaderTransitive = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#broaderTransitive" );
    
    ObjectProperty closeMatch = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#closeMatch" );
    
    /**
     * skos:exactMatch is disjoint with each of the properties skos:broadMatch and
     *  skos:relatedMatch.
     */
    ObjectProperty exactMatch = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#exactMatch" );
    
    ObjectProperty hasTopConcept = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#hasTopConcept" );
    
    ObjectProperty inScheme = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#inScheme" );
    
    /**
     * These concept mapping relations mirror semantic relations, and the data model
     * defined below is similar (with the exception of skos:exactMatch) to the data
     * model defined for semantic relations. A distinct vocabulary is provided for
     * concept mapping relations, to provide a convenient way to differentiate links
     * within a concept scheme from links between concept schemes. However, this
     * pattern of usage is not a formal requirement of the SKOS data model, and relies
     * on informal definitions of best practice.
     */
    ObjectProperty mappingRelation = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#mappingRelation" );
    
    ObjectProperty member = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#member" );
    
    /**
     * For any resource, every item in the list given as the value of the skos:memberList
     * property is also a value of the skos:member property.
     */
    ObjectProperty memberList = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#memberList" );
    
    ObjectProperty narrowMatch = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#narrowMatch" );
    
    /**
     * Narrower concepts are typically rendered as children in a concept hierarchy (tree).
     */
    ObjectProperty narrower = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#narrower" );
    
    ObjectProperty narrowerTransitive = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#narrowerTransitive" );
    
    /**
     * skos:related is disjoint with skos:broaderTransitive
     */
    ObjectProperty related = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#related" );
    
    ObjectProperty relatedMatch = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#relatedMatch" );
    
    ObjectProperty semanticRelation = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#semanticRelation" );
    
    ObjectProperty topConceptOf = m_model.createObjectProperty( "http://www.w3.org/2004/02/skos/core#topConceptOf" );
    
    DatatypeProperty notation = m_model.createDatatypeProperty( "http://www.w3.org/2004/02/skos/core#notation" );
    
    /**
     * The range of skos:altLabel is the class of RDF plain literals.skos:prefLabel,
     * skos:altLabel and skos:hiddenLabel are pairwise disjoint properties.
     */
    AnnotationProperty altLabel = m_model.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#altLabel" );
    
    AnnotationProperty changeNote = m_model.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#changeNote" );
    
    AnnotationProperty definition = m_model.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#definition" );
    
    AnnotationProperty editorialNote = m_model.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#editorialNote" );
    
    AnnotationProperty example = m_model.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#example" );
    
    /**
     * The range of skos:hiddenLabel is the class of RDF plain literals.skos:prefLabel,
     *  skos:altLabel and skos:hiddenLabel are pairwise disjoint properties.
     */
    AnnotationProperty hiddenLabel = m_model.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#hiddenLabel" );
    
    AnnotationProperty historyNote = m_model.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#historyNote" );
    
    AnnotationProperty note = m_model.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#note" );
    
    /**
     * A resource has no more than one value of skos:prefLabel per language tag.skos:prefLabel,
     * skos:altLabel and skos:hiddenLabel are pairwise disjoint properties.The range
     * of skos:prefLabel is the class of RDF plain literals.
     */
    AnnotationProperty prefLabel = m_model.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#prefLabel" );
    
    AnnotationProperty scopeNote = m_model.createAnnotationProperty( "http://www.w3.org/2004/02/skos/core#scopeNote" );
    
    OntClass Collection = m_model.createClass( "http://www.w3.org/2004/02/skos/core#Collection" );
    
    OntClass Concept = m_model.createClass( "http://www.w3.org/2004/02/skos/core#Concept" );
    
    OntClass ConceptScheme = m_model.createClass( "http://www.w3.org/2004/02/skos/core#ConceptScheme" );
    
    OntClass OrderedCollection = m_model.createClass( "http://www.w3.org/2004/02/skos/core#OrderedCollection" );
}
