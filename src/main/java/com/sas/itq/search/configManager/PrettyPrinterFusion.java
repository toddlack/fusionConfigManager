package com.sas.itq.search.configManager;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;

/**
 * JSON pretty printer to mimic json coming back from calls in Postman, the api tool.
 */
public class PrettyPrinterFusion extends DefaultPrettyPrinter {
    /**
     * Method called after a objects-level value has been completely
     * output, and before another value is to be output.
     * <p>
     * Default
     * handling (without pretty-printing) will output a space, to
     * allow values to be parsed correctly. Pretty-printer is
     * to output some other suitable and nice-looking separator
     * (tab(s), space(s), linefeed(s) or any combination thereof).
     *
     * @param gen
     */
    @Override
    public void writeRootValueSeparator(JsonGenerator gen) throws IOException {
        super.writeRootValueSeparator(gen);
    }

    /**
     * Method called when an Object value is to be output, before
     * any fields are output.
     * <p>
     * Default handling (without pretty-printing) will output
     * the opening curly bracket.
     * Pretty-printer is
     * to output a curly bracket as well, but can surround that
     * with other (white-space) decoration.
     *
     * @param gen
     */
    @Override
    public void writeStartObject(JsonGenerator gen) throws IOException {
        super.writeStartObject(gen);
    }

    /**
     * Method called after an Object value has been completely output
     * (minus closing curly bracket).
     * <p>
     * Default handling (without pretty-printing) will output
     * the closing curly bracket.
     * Pretty-printer is
     * to output a curly bracket as well, but can surround that
     * with other (white-space) decoration.
     *
     * @param gen
     * @param nrOfEntries Number of direct members of the array that
     */
    @Override
    public void writeEndObject(JsonGenerator gen, int nrOfEntries) throws IOException {
        super.writeEndObject(gen, nrOfEntries);
    }

    /**
     * Method called after an object entry (field:value) has been completely
     * output, and before another value is to be output.
     * <p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate the two. Pretty-printer is
     * to output a comma as well, but can surround that with other
     * (white-space) decoration.
     *
     * @param gen
     */
    @Override
    public void writeObjectEntrySeparator(JsonGenerator gen) throws IOException {
        super.writeObjectEntrySeparator(gen);

    }

    /**
     * Method called after an object field has been output, but
     * before the value is output.
     * <p>
     * Default handling (without pretty-printing) will output a single
     * colon to separate the two. Pretty-printer is
     * to output a colon as well, but can surround that with other
     * (white-space) decoration.
     *
     * @param gen
     */
    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator gen) throws IOException {
        super.writeObjectFieldValueSeparator(gen);
    }

    /**
     * Method called when an Array value is to be output, before
     * any member/child values are output.
     * <p>
     * Default handling (without pretty-printing) will output
     * the opening bracket.
     * Pretty-printer is
     * to output a bracket as well, but can surround that
     * with other (white-space) decoration.
     *
     * @param gen
     */
    @Override
    public void writeStartArray(JsonGenerator gen) throws IOException {
        super.writeStartArray(gen);

    }

    /**
     * Method called after an Array value has been completely output
     * (minus closing bracket).
     * <p>
     * Default handling (without pretty-printing) will output
     * the closing bracket.
     * Pretty-printer is
     * to output a bracket as well, but can surround that
     * with other (white-space) decoration.
     *
     * @param gen
     * @param nrOfValues Number of direct members of the array that
     */
    @Override
    public void writeEndArray(JsonGenerator gen, int nrOfValues) throws IOException {
        super.writeEndArray(gen, nrOfValues);

    }

    /**
     * Method called after an array value has been completely
     * output, and before another value is to be output.
     * <p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate the two. Pretty-printer is
     * to output a comma as well, but can surround that with other
     * (white-space) decoration.
     *
     * @param gen
     */
    @Override
    public void writeArrayValueSeparator(JsonGenerator gen) throws IOException {
        super.writeArrayValueSeparator(gen);

    }

    /**
     * Method called after array start marker has been output,
     * and right before the first value is to be output.
     * It is <b>not</b> called for arrays with no values.
     * <p>
     * Default handling does not output anything, but pretty-printer
     * is free to add any white space decoration.
     *
     * @param gen
     */
    @Override
    public void beforeArrayValues(JsonGenerator gen) throws IOException {
        super.beforeArrayValues(gen);

    }

    /**
     * Method called after object start marker has been output,
     * and right before the field name of the first entry is
     * to be output.
     * It is <b>not</b> called for objects without entries.
     * <p>
     * Default handling does not output anything, but pretty-printer
     * is free to add any white space decoration.
     *
     * @param gen
     */
    @Override
    public void beforeObjectEntries(JsonGenerator gen) throws IOException {
        super.beforeObjectEntries(gen);
    }

}
