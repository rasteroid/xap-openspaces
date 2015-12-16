/*******************************************************************************
 *
 * Copyright (c) 2015 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package org.openspaces.spatial.shapes;

/**
 * A polygon, denoted by 3 or more points
 *
 * @author Yohana Khoury
 * @since 11.0
 */
public interface Polygon extends Shape {

    /**
     * Returns the number of points within the polygon
     * @return The number of points within the polygon
     */
    int getNumOfPoints();

    /**
     * Gets the X coordinate of the point in the specified index.
     */
    double getX(int index);

    /**
     * Gets the Y coordinate of the point in the specified index.
     */
    double getY(int index);
}
