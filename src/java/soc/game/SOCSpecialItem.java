/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2014 Jeremy D Monin <jeremy@nand.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soc.game;

import java.util.ArrayList;
import java.util.List;


/**
 * A special item for Settlers scenarios or expansions.
 *<P>
 * Example use: The Wonders chosen by players in the {@code _SC_WOND} scenario.
 *<P>
 * Special Items are per-game and/or per-player.  In {@link SOCGame} and {@link SOCPlayer}
 * they're accessed by an item type key.  For compatibility among scenarios and expansions,
 * this key should be a {@link SOCGameOption} keyname; if an option has more than one
 * special item type, {@code typeKey} should be optionName + "/" + a short alphanumeric key of your choosing.
 * Please document the Special Item type(s) in the SOCGameOption's javadoc, including
 * whether each is per-game, per-player, or both (for more convenient access).
 *<P>
 * In some scenarios, Special Items may have requirements for players to build or use them.
 * See {@link SOCSpecialItem.Requirement} javadoc for more details.  To check requirements,
 * call {@link SOCSpecialItem#checkRequirements(SOCPlayer, String)}.
 *<P>
 * <B>Non-Networked Fields:</B><BR>
 * The cost and requirement fields are initialized at the server and at the client, not sent over the network.
 * Because of their limited and known use, it's easier to set them up in a factory method here than to create,
 * send, and parse messages with all details of the game's Special Items.  If a new Special Item type is created
 * for a new scenario or expansion, the client would most likely need new code to handle that scenario or
 * expansion, so the new item type's field initialization can be added to the factory at that time.
 * See {@link #makeKnownItem(String, int)}.
 *<P>
 * <B>Locks:</B> Field values are not synchronized here. If a specific item type or access pattern
 * requires synchronization, do so outside this class and document the details.
 *<P>
 * Special items must be {@link Cloneable} for use in copy constructors, see {@link #clone()} for details.
 *
 * @since 2.0.00
 */
public class SOCSpecialItem
    implements Cloneable
{

    /**
     * Requirements for the Wonders in the {@link SOCGameOption#K_SC_WOND _SC_WOND} scenario.
     * Index 0 unused.  The 6-player game includes another copy of the first two wonders.
     * Used by {@link #makeKnownItem(String, int)}.
     */
    private static final String[] REQ_SC_WOND = { null, "2C", "S@N2", "C@P,5L", "S@N1", "C,6V", "2C", "S@N2" };

    /**
     * Costs for the Wonders in the {@link SOCGameOption#K_SC_WOND _SC_WOND} scenario.
     * Index 0 unused.  The 6-player game includes another copy of the first two wonders.
     * Each 5-element array is { clay, ore, sheep, wheat, wood }. Used by {@link #makeKnownItem(String, int)}.
     */
    private static final int[][] COST_SC_WOND =
    {
        null,
        // clay, ore, sheep, wheat, wood
        { 1, 0, 3, 0, 1 },  // theater
        { 0, 0, 1, 1, 3 },  // great bridge
        { 0, 2, 0, 3, 0 },  // monument
        { 3, 0, 0, 1, 1 },  // great wall
        { 1, 3, 0, 1, 0 },  // cathedral
        { 1, 0, 3, 0, 1 },  // theater
        { 0, 0, 1, 1, 3 }   // great bridge
    };

    /**
     * Create a scenario/expansion's special item if known. This is a factory method for game setup convenience.
     * The known item's {@link #req requirements} and cost will be filled from static data.
     *<P>
     * Currently known {@code typeKey}s:
     *<UL>
     *<LI> {@link SOCGameOption#K_SC_WOND _SC_WOND}: Wonders
     *</UL>
     * If {@code typeKey} is unknown, the item will be created with {@code null} cost and requirements,
     * equivalent to calling {@link #SOCSpecialItem(SOCPlayer, int, SOCResourceSet, String) new SOCSpecialItem}
     * {@code (null, -1, null, null)}.
     *
     * @param typeKey  Special item type.  Typically a {@link SOCGameOption} keyname;
     *    see {@link SOCSpecialItem class javadoc} for details.
     * @param idx  Index within game's Special Item list
     * @return A Special Item at no coordinate (-1) and unowned by any player, with cost/requirements if known,
     *     or {@code null} cost and requirements otherwise.
     */
    public static final SOCSpecialItem makeKnownItem(final String typeKey, final int idx)
    {
        if (! typeKey.equals(SOCGameOption.K_SC_WOND))
        {
            return new SOCSpecialItem(null, -1, null, null);  // <--- Early return: Unknown typeKey ---
        }

        final String[] typeReqs = REQ_SC_WOND;
        final int[][] typeCosts = COST_SC_WOND;

        final SOCResourceSet costRS;
        if ((idx < 0) || (idx >= typeCosts.length))
        {
            costRS = null;
        } else {
            final int[] cost = typeCosts[idx];
            costRS = (cost == null) ? null : new SOCResourceSet(cost);
        }

        final String req = ((idx < 0) || (idx >= typeReqs.length)) ? null : typeReqs[idx];

        return new SOCSpecialItem(null, -1, costRS, req);
    }

    /**
     * The player who owns this item, if any. Will be null for certain items
     * which belong to the game and not to players.
     */
    protected SOCPlayer player;

    /** Optional coordinates on the board for this item, or -1. An edge or a node, depending on item type. */
    protected int coord;

    /** Optional level of construction or strength, or 0. */
    protected int level;

    /**
     * Optional cost to buy, use, or build the next level, or {@code null}.
     * Not sent over the network; see {@link SOCSpecialItem class javadoc}.
     */
    protected SOCResourceSet cost;

    /**
     * Optional requirements to buy, use, or build the next level, or {@code null}.
     * Not sent over the network; see {@link SOCSpecialItem class javadoc}.
     */
    public final List<Requirement> req;

    /**
     * Make a new item, optionally owned by a player.
     * Its optional Level will be 0.
     *
     * @param pl  player who owns the item, or {@code null}
     * @param co  coordinates, or -1
     * @param cost  cost to buy, use, or build the next level, or null
     * @param req  requirements to buy, use, or build the next level, or null.
     *      If provided, this requirement specification string will be
     *      parsed by {@link SOCSpecialItem.Requirement#parse(String)}.
     * @throws IllegalArgumentException  if {@code req != null} but isn't a syntactically valid specification
     */
    public SOCSpecialItem(SOCPlayer pl, final int co, SOCResourceSet cost, final String req)
        throws IllegalArgumentException
    {
        this(pl, co, 0, cost, req);
    }

    /**
     * Make a new item, optionally owned by a player, with a level.
     *
     * @param pl  player who owns the item, or {@code null}
     * @param co  coordinates, or -1
     * @param lv  current level of construction or strength, or 0
     * @param cost  cost to buy, use, or build the next level, or null
     * @param req  requirements to buy, use, or build the next level, or null.
     *      If provided, this requirement specification string will be
     *      parsed by {@link SOCSpecialItem.Requirement#parse(String)}.
     * @throws IllegalArgumentException  if {@code req != null} but isn't a syntactically valid specification
     */
    public SOCSpecialItem(SOCPlayer pl, final int co, final int lv, SOCResourceSet cost, final String req)
        throws IllegalArgumentException
    {
        player = pl;
        coord = co;
        level = lv;
        this.cost = cost;
        this.req = (req != null) ? Requirement.parse(req) : null;
    }

    /**
     * Get the player who owns this item, if any.
     * @return the owner of the item, or {@code null}
     */
    public SOCPlayer getPlayer()
    {
        return player;
    }

    /**
     * Set or clear the player who owns this item.
     * @param pl  the owner of this item, or {@code null}
     */
    public void setPlayer(SOCPlayer pl)
    {
        player = pl;
    }

    /**
     * @return the node or edge coordinate for this item, or -1 if none
     */
    public int getCoordinates()
    {
        return coord;
    }

    /**
     * @param co the node or edge coordinate for this item, or -1 if none
     */
    public void setCoordinates(final int co)
    {
        coord = co;
    }

    /**
     * Get the current construction level or strength of this item.
     * @return  Current level
     */
    public int getLevel()
    {
        return level;
    }

    /**
     * Set the current level of this special item.
     * @param lv  New level
     */
    public void setLevel(final int lv)
    {
        level = lv;
    }

    /**
     * Get the optional cost to buy, use, or build the next level.
     * Not sent over the network; see {@link SOCSpecialItem class javadoc}.
     * @return  Cost, or {@code null}
     */
    public SOCResourceSet getCost()
    {
        return cost;
    }

    /**
     * Set or clear the optional cost to buy, use, or build the next level.
     * Not sent over the network; see {@link SOCSpecialItem class javadoc}.
     * @param co  New cost, or {@code null} to clear
     */
    public void setCost(SOCResourceSet co)
    {
        cost = co;
    }

    /**
     * Does this player meet a special item's requirements?
     *
     * @param pl  Player to check
     * @param reqs  Requirements string; for syntax see {@link SOCSpecialItem.Requirement#parse(String)}
     * @return  True if player meets the requirements, false otherwise; true if {@code reqs} is ""
     * @throws IllegalArgumentException if {@code reqs} has incorrect syntax, or refers to an Added Layout Part
     *     {@code "N1"} through {@code "N9"} that isn't defined in the board layout
     * @throws UnsupportedOperationException if requirement type S (Settlement) includes {@code atPort} location;
     *     this is not implemented
     */
    public static boolean checkRequirements(final SOCPlayer pl, final String reqs)
        throws IllegalArgumentException, UnsupportedOperationException
    {
        final List<Requirement> reqsList = Requirement.parse(reqs);
        if (reqsList == null)
            return true;  // no requirements, nothing to fail

        for (final Requirement req : reqsList)
        {
            final int count = req.count;
            List<? extends SOCPlayingPiece> pieces = null;

            switch (req.reqType)
            {
            case 'S':
                pieces = pl.getSettlements();
                if (pieces.size() < count)
                    return false;
                break;

            case 'C':
                pieces = pl.getCities();
                if (pieces.size() < count)
                    return false;
                break;

            case 'V':
                if (pl.getTotalVP() < count)
                    return false;
                break;

            case 'L':
                if (pl.getLongestRoadLength() < count)
                    return false;
                break;

            default:
                throw new IllegalArgumentException("Unknown requirement type " + req.reqType);
            }

            if (pieces != null)
            {
                // check for location requirement:

                if (req.atPort && (req.reqType == 'S'))
                    throw new UnsupportedOperationException("atPort reqType S not implemented");

                if (req.atCoordList != null)
                {
                    boolean foundAtNode = false;
                    final int[] nodes = ((SOCBoardLarge) pl.getGame().getBoard()).getAddedLayoutPart(req.atCoordList);
                    if (nodes == null)
                        throw new IllegalArgumentException
                            ("Requirement uses undefined Added Layout Part " + req.atCoordList);

                    for (SOCPlayingPiece pp : pieces)
                    {
                        final int node = pp.getCoordinates();
                        for (int i = 0; i < nodes.length; ++i)
                        {
                            if (node == nodes[i])
                            {
                                foundAtNode = true;
                                break;
                            }
                        }

                        if (foundAtNode)
                            break;
                    }

                    if (! foundAtNode)
                        return false;  // no matching piece found
                }
                else if (req.atPort)
                {
                    boolean foundAtNode = false;
                    final SOCBoard board = pl.getGame().getBoard();

                    for (SOCPlayingPiece pp : pieces)
                    {
                        if (board.getPortTypeFromNodeCoord(pp.getCoordinates()) != -1)
                        {
                            foundAtNode = true;
                            break;
                        }
                    }

                    if (! foundAtNode)
                        return false;  // no matching piece found
                }
            }
        }

        return true;  // didn't fail any requirement
    }

    /**
     * @return a human readable form of this object
     */
    @Override
    public String toString()
    {
        return "SOCSpecialItem:player=" + player + "|coord=" + Integer.toHexString(coord) + "|level=" + level
            + "|cost=[" + cost + "]|req=" + req;
    }

    /**
     * Compare this SOCSpecialItem to another SOCSpecialItem, or another object.
     * Comparison method:
     * <UL>
     * <LI> If other is null, false.
     * <LI> If other is not a SOCSpecialItem, use our super.equals to compare.
     * <LI> SOCSpecialItem are equal with the same coordinate, player, and level.
     * </UL>
     *
     * @param other The object to compare with, or null
     */
    @Override
    public boolean equals(Object other)
    {
        if (other == null)
            return false;
        if (! (other instanceof SOCSpecialItem))
            return super.equals(other);

        return ((coord == ((SOCSpecialItem) other).coord)
            &&  (player == ((SOCSpecialItem) other).player)
            &&  (level == ((SOCSpecialItem) other).level));
    }

    /**
     * For use in set copy constructors, create and return a clone of this {@link SOCSpecialItem}.
     * The {@code SOCSpecialItem} implementation just calls {@code super.clone()}.
     * If subclasses have any lists or structures, be sure to deeply copy them.
     * Requirements aren't deep-copied, because they are final and won't change.
     * @throws CloneNotSupportedException  Declared from super.clone(), should not occur
     *     since SOCSpecialItem implements Cloneable.
     * @return a clone of this item
     */
    public SOCSpecialItem clone()
        throws CloneNotSupportedException
    {
        SOCSpecialItem cl = (SOCSpecialItem) super.clone();
        cl.cost = cost.copy();
        return cl;
    }

    /**
     * Data structure and parser for a special item's requirements.
     *<P>
     * A requirement is a minimum count of items (Settlements, Cities, Victory Points, or Length of player's longest
     * route) with an optional required position (at a Port, or at a list of special nodes) for at least one of the
     * Settlement or City items.
     *
     * @see #parse(String)
     * @see SOCSpecialItem#checkRequirements(SOCPlayer, String)
     */
    public static final class Requirement
    {
        /** 'S' for settlement, 'C' for city, 'V' for victory points, 'L' for length of player's longest route */
        public final char reqType;

        /** Number of pieces, victory points, or length of route required */
        public final int count;

        /**
         * If true, a {@code reqType} piece must be at a 3:1 or 2:1 port.
         * Currently, only {@code reqType} C (City) is supported here,
         * because no current scenario justified the extra coding for S-or-C.
         */
        public final boolean atPort;

        /**
         * Board layout coordinate list such as "N1", or null.  If non-null, a reqType piece must
         * be at a node coordinate in this named list within the board layout's {@code getAddedLayoutPart}s.
         */
        public final String atCoordList;

        /**
         * Parse a requirement specification string into {@link Requirement} objects.
         *<P>
         * Requirements are a comma-separated list of items, each item having this syntax:<BR>
         * [count] itemType [@ location]
         *<UL>
         * <LI> Count is an optional integer, otherwise 1 is the default
         * <LI> itemType is a letter for the requirement type:
         *    C for Cities, S for Settlements, V for total Victory Points,
         *    or L for length of player's longest trade route.
         * <LI> Location is an optional location that one of the player's {@code itemType} pieces must be at:
         *    P for any 3:1 or 2:1 trade port, or N1 through N9 for a Node List in the board layout
         *    ({@link SOCBoardLarge#getAddedLayoutPart(String) board.getAddedLayoutPart("N1")} etc).
         *</UL>
         *
         * <H5>Examples:</H5>
         *<UL>
         * <LI>{@code 3S} = 3 settlements
         * <LI>{@code 2C,8V} = 2 cities, 8 victory points
         * <LI>{@code 6L} = trade route length 6
         * <LI>{@code S@P} = settlement at any port
         * <LI>{@code 2C@N2} = 2 cities, at least one of which is in node list 2 ({@code "N2"}) in the board layout
         *</UL>
         *
         * @param req  Requirements string following the syntax given above
         * @return List of {@link Requirement}s, or {@code null} if {@code req} is ""
         * @throws IllegalArgumentException  if {@code req} isn't a syntactically valid specification
         */
        public static List<Requirement> parse(final String req)
            throws IllegalArgumentException
        {
            final int L = req.length();
            if (L == 0)
                return null;

            ArrayList<Requirement> ret = new ArrayList<Requirement>();

            int i = 0;
            char c;  // in parsing loop, c == req.charAt(i)

            // Loop for each comma-separated requirement
            while (i < L)
            {
                c = req.charAt(i);

                // first: optional digit(s), then item-type letter
                int itemCount;
                if (Character.isDigit(c))
                {
                    int j = i + 1;
                    while (j < req.length())
                    {
                        c = req.charAt(j);
                        if (! Character.isDigit(c))
                            break;

                        ++j;
                    }
                    if (j == req.length())
                        throw new IllegalArgumentException("Must follow item count with item type in " + req);
                    // postcondition: j is 1 char past end of digits

                    itemCount = Integer.parseInt(req.substring(i, j));
                    i = j;
                    // c was req.charAt(j) already
                } else {
                    itemCount = 1;
                }

                final char reqType = c;  // 'S', 'C', 'V', 'L'
                if ((c < 'A') || (c > 'Z'))
                    throw new IllegalArgumentException("Expected item-type letter at position " + i + " in " + req);

                ++i;
                if (i >= L)
                {
                    // This req is done, comma separates it from next req
                    ret.add(new Requirement(reqType, itemCount, false, null));

                    break;  // <--- Finished last req ---
                }

                c = req.charAt(i);

                if (c == ',')
                {
                    // This req is done: comma separates it from next req
                    ret.add(new Requirement(reqType, itemCount, false, null));

                    ++i;
                    continue;
                }

                if (c != '@')
                    throw new IllegalArgumentException("Expected @ or , at position " + i + " in " + req);

                ++i;
                if (i >= L)
                    throw new IllegalArgumentException("Must follow @ with P or N# in " + req);

                c = req.charAt(i);

                // Currently valid after '@': N#, or P for Port
                switch (c)
                {
                case 'P':
                    ret.add(new Requirement(reqType, itemCount, true, null));
                    ++i;
                    break;

                case 'N':
                    ++i;
                    if (i < L)
                        c = req.charAt(i);
                    if ((i < L) && Character.isDigit(c))
                    {
                        ret.add(new Requirement(reqType, itemCount, false, "N" + c));
                        ++i;
                        break;
                    }
                    // else, will fall through to default and throw exception

                default:
                    throw new IllegalArgumentException("Must follow @ with P or N# in " + req);
                }

                // If we got here, we've parsed the req and i should be at end or comma
                if (i < L)
                {
                    if (req.charAt(i) != ',')
                        throw new IllegalArgumentException
                            ("Extra characters in spec: Expected , or end of string at position " + i + " in " + req);

                    ++i;
                    // top of main loop will parse the requirement that follows the comma
                }
            }

            return ret;
        }

        /**
         * Create a Requirement item with these field values.
         * See each field's javadoc for meaning of parameter named from that field.
         * Parameter values are not validated here.
         * @param reqType  See {@link #reqType}
         * @param count    See {@link #count}
         * @param atPort   See {@link #atPort}
         * @param atCoordList  See {@link #atCoordList}
         */
        public Requirement(final char reqType, final int count, final boolean atPort, final String atCoordList)
        {
            this.reqType = reqType;
            this.count = count;
            this.atPort = atPort;
            this.atCoordList = atCoordList;
        }
    }

}
