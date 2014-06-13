package org.wikidata.wdtk.dumpfiles.constraint.parser;

/*
 * #%L
 * Wikidata Toolkit Dump File Handling
 * %%
 * Copyright (C) 2014 Wikidata Toolkit Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.Validate;
import org.wikidata.wdtk.datamodel.implementation.DataObjectFactoryImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.dumpfiles.constraint.model.Constraint;
import org.wikidata.wdtk.dumpfiles.constraint.model.PropertyValues;
import org.wikidata.wdtk.dumpfiles.constraint.template.Template;
import org.wikidata.wdtk.dumpfiles.constraint.template.TemplateConstant;

/**
 * 
 * @author Julian Mendez
 * 
 */
public class ConstraintMainParser implements ConstraintParser {

	public static final String PREFIX_WIKIDATA = "http://www.wikidata.org/entity/";

	final Map<String, ConstraintParser> mapOfParsers = new HashMap<String, ConstraintParser>();

	public ConstraintMainParser() {
		registerIds();
	}

	static String removeBrackets(String str) {
		Validate.notNull(str);
		return str.replace(TemplateConstant.OPENING_BRACKETS, "")
				.replace(TemplateConstant.CLOSING_BRACKETS, "")
				.replace(TemplateConstant.OPENING_BRACES, "")
				.replace(TemplateConstant.CLOSING_BRACES, "")
				.replace(TemplateConstant.VERTICAL_BAR, "");
	}

	static List<ItemIdValue> parseListOfItems(String listOfItems) {
		Validate.notNull(listOfItems);
		List<ItemIdValue> ret = new ArrayList<ItemIdValue>();
		String str = removeBrackets(listOfItems);
		DataObjectFactoryImpl factory = new DataObjectFactoryImpl();
		StringTokenizer stok = new StringTokenizer(str, TemplateConstant.COMMA);
		while (stok.hasMoreTokens()) {
			String itemStr = stok.nextToken().trim();
			ItemIdValue item = factory.getItemIdValue(itemStr.toUpperCase(),
					ConstraintMainParser.PREFIX_WIKIDATA);
			ret.add(item);
		}
		return ret;
	}

	static List<PropertyValues> parseListOfPropertyValues(String listOfItems) {
		Validate.notNull(listOfItems);
		List<PropertyValues> ret = new ArrayList<PropertyValues>();
		String str = removeBrackets(listOfItems);
		DataObjectFactoryImpl factory = new DataObjectFactoryImpl();
		StringTokenizer stok = new StringTokenizer(str,
				TemplateConstant.SEMICOLON);
		while (stok.hasMoreTokens()) {
			String propertyValuesStr = stok.nextToken().trim();
			int pos = propertyValuesStr.indexOf(TemplateConstant.COLON);
			if (pos == -1) {
				PropertyIdValue property = factory.getPropertyIdValue(
						propertyValuesStr.toUpperCase(),
						ConstraintMainParser.PREFIX_WIKIDATA);
				ret.add(new PropertyValues(property));
			} else {
				PropertyIdValue property = factory.getPropertyIdValue(
						propertyValuesStr.substring(0, pos).trim()
								.toUpperCase(),
						ConstraintMainParser.PREFIX_WIKIDATA);
				List<ItemIdValue> values = parseListOfItems(propertyValuesStr
						.substring(pos + 1));
				ret.add(new PropertyValues(property, values));
			}
		}
		return ret;
	}

	static List<Integer> parseListOfQuantities(String listOfQuantities) {
		Validate.notNull(listOfQuantities);
		List<Integer> ret = new ArrayList<Integer>();
		StringTokenizer stok = new StringTokenizer(listOfQuantities,
				TemplateConstant.COMMA);
		while (stok.hasMoreTokens()) {
			ret.add(Integer.parseInt(stok.nextToken()));
		}
		return ret;
	}

	/**
	 * Creates a constraint based on a template, or <code>null</code> if the
	 * template does not correspond to a known constraint
	 * 
	 * @param template
	 *            template
	 * @return a constraint based on a template, or <code>null</code> if the
	 *         template does not correspond to a known constraint
	 */
	@Override
	public Constraint parse(PropertyIdValue constrainedProperty,
			Template template) {
		Validate.notNull(constrainedProperty);
		Validate.notNull(template);
		Constraint ret = null;
		String templateId = normalize(template.getName());
		String prefix = normalize(ConstraintParserConstant.T_CONSTRAINT);
		if (templateId.startsWith(prefix)) {
			String constraintId = normalize(templateId.substring(prefix
					.length()));
			ConstraintParser constraintParser = getConstraintParser(constraintId);
			if (constraintParser != null) {
				ret = constraintParser.parse(constrainedProperty, template);
			}
		}
		return ret;
	}

	public ConstraintParser getConstraintParser(String str) {
		return this.mapOfParsers.get(str);
	}

	public String normalize(String str) {
		String ret = "";
		if (str != null) {
			ret = str
					.trim()
					.toLowerCase()
					.replace(TemplateConstant.UNDERSCORE,
							TemplateConstant.SPACE);
			if (ret.length() > 0) {
				ret = ret.substring(0, 1).toUpperCase() + ret.substring(1);
			}
		}
		return ret;
	}

	private void register(String str, ConstraintParser parser) {
		this.mapOfParsers.put(normalize(str), parser);
	}

	private void registerIds() {
		register(ConstraintParserConstant.C_SINGLE_VALUE,
				new ConstraintSingleValueParser());
		register(ConstraintParserConstant.C_UNIQUE_VALUE,
				new ConstraintUniqueValueParser());
		register(ConstraintParserConstant.C_FORMAT,
				new ConstraintFormatParser());
		register(ConstraintParserConstant.C_ONE_OF, new ConstraintOneOfParser());
		register(ConstraintParserConstant.C_SYMMETRIC,
				new ConstraintSymmetricParser());
		register(ConstraintParserConstant.C_INVERSE,
				new ConstraintInverseParser());
		register(ConstraintParserConstant.C_EXISTING_FILE,
				new ConstraintExistingFileParser());
		register(ConstraintParserConstant.C_TARGET_REQUIRED_CLAIM,
				new ConstraintTargetRequiredClaimParser());
		register(ConstraintParserConstant.C_ITEM, new ConstraintItemParser());
		register(ConstraintParserConstant.C_TYPE, new ConstraintTypeParser());
		register(ConstraintParserConstant.C_VALUE_TYPE,
				new ConstraintValueTypeParser());
		register(ConstraintParserConstant.C_RANGE, new ConstraintRangeParser());
		register(ConstraintParserConstant.C_MULTI_VALUE,
				new ConstraintMultiValueParser());
		register(ConstraintParserConstant.C_CONFLICTS_WITH,
				new ConstraintConflictsWithParser());
		register(ConstraintParserConstant.C_QUALIFIER,
				new ConstraintQualifierParser());
		register(ConstraintParserConstant.C_PERSON,
				new ConstraintPersonParser());
		register(ConstraintParserConstant.C_TAXON, new ConstraintTaxonParser());
	}

}
