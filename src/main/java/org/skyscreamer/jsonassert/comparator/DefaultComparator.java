/*
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

package org.skyscreamer.jsonassert.comparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import static org.skyscreamer.jsonassert.comparator.JSONCompareUtil.allJSONObjects;
import static org.skyscreamer.jsonassert.comparator.JSONCompareUtil.allSimpleValues;

/**
 * This class is the default json comparator implementation.
 * Comparison is performed according to {@link JSONCompareMode} that is passed as constructor's argument.
 */
public class DefaultComparator extends AbstractComparator {

    JSONCompareMode mode;

    public DefaultComparator(JSONCompareMode mode) {
        this.mode = mode;
    }

    @Override
    public void compareJSON(String prefix, JSONObject expected, JSONObject actual, JSONCompareResult result)
            throws JSONException {
        // Check that actual contains all the expected values
        checkJsonObjectKeysExpectedInActual(prefix, expected, actual, result);

        // If strict, check for vice-versa
        if (!mode.isExtensible()) {
            checkJsonObjectKeysActualInExpected(prefix, expected, actual, result);
        }
    }

    @Override
    public void compareValues(String prefix, Object expectedValue, Object actualValue, JSONCompareResult result)
            throws JSONException {
        if (expectedValue == actualValue) {
            return;
        }
        if ((expectedValue == null && actualValue != null) || (expectedValue != null && actualValue == null)) {
            result.fail(prefix, expectedValue, actualValue);
        }
        if (areNumbers(expectedValue, actualValue)) {
            if (areNotSameDoubles(expectedValue, actualValue)) {
                result.fail(prefix, expectedValue, actualValue);
            }
        } else if (expectedValue.getClass().isAssignableFrom(actualValue.getClass())) {
            if (expectedValue instanceof JSONArray) {
                compareJSONArray(prefix, (JSONArray) expectedValue, (JSONArray) actualValue, result);
            } else if (expectedValue instanceof JSONObject) {
                compareJSON(prefix, (JSONObject) expectedValue, (JSONObject) actualValue, result);
            } else if (!expectedValue.equals(actualValue)) {
                result.fail(prefix, expectedValue, actualValue);
            }
        } else {
            result.fail(prefix, expectedValue, actualValue);
        }
    }

    /**
     * Generate error message for JSONArray comparision.<br>
     *
     * Solve problem mentioned in <a href="https://github.com/skyscreamer/JSONassert/issues/128">issue 128</a>
     * @author Chengqi Xu
     * @param prefix The prefix description given in compareJSONArray
     * @param expected The expected length of the JSONArray
     * @param actual The actual length of the JSONArray
     * @return The description used as error message for JSONArray comparision
     **/
    // CS304 Issue link: https://github.com/skyscreamer/JSONassert/issues/128
    public String getMessage(String prefix, int expected, int actual){
        StringBuilder msgBuilder = new StringBuilder(prefix);
        String description1 = " values but got ";
        String description2 = "";
        if (actual > 1) {
            description2 = " values.";
        }
        else {
            description2 = " value.";
        }
        if (expected <= 1) {
            description1 = " value but got ";
        }
        msgBuilder.append("[]: Expected ");
        msgBuilder.append(expected);
        msgBuilder.append(description1);
        msgBuilder.append(actual);
        msgBuilder.append(description2);
        return msgBuilder.toString();
    }

    /**
     *
     * @param prefix   the path in the json where the comparison happens
     * @param expected the expected JSON array
     * @param actual   the actual JSON array
     * @param result   stores the actual state of the comparison result
     * @throws JSONException Used to indicate the error
     */
    // CS304 Issue link: https://github.com/skyscreamer/JSONassert/issues/128
    @Override
    public void compareJSONArray(String prefix, JSONArray expected, JSONArray actual, JSONCompareResult result)
            throws JSONException {
        if (expected.length() != actual.length()) {
            result.fail(getMessage(prefix,expected.length(), actual.length()));
            return;
        } else if (expected.length() == 0) {
            return; // Nothing to compare
        }

        if (mode.hasStrictOrder()) {
            compareJSONArrayWithStrictOrder(prefix, expected, actual, result);
        } else if (allSimpleValues(expected)) {
            compareJSONArrayOfSimpleValues(prefix, expected, actual, result);
        } else if (allJSONObjects(expected)) {
            compareJSONArrayOfJsonObjects(prefix, expected, actual, result);
        } else {
            // An expensive last resort
            recursivelyCompareJSONArray(prefix, expected, actual, result);
        }
    }

    protected boolean areNumbers(Object expectedValue, Object actualValue) {
        return expectedValue instanceof Number && actualValue instanceof Number;
    }

    protected boolean areNotSameDoubles(Object expectedValue, Object actualValue) {
        return ((Number) expectedValue).doubleValue() != ((Number) actualValue).doubleValue();
    }
}
