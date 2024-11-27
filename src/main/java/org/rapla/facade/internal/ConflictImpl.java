/*--------------------------------------------------------------------------*
 | Copyright (C) 2013 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.facade.internal;

import org.jetbrains.annotations.Nullable;
import org.rapla.components.util.DateTools;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.internal.AllocatableImpl;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.entities.internal.ModifiableTimestamp;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.ReferenceInfo;
import org.rapla.entities.storage.UnresolvableReferenceExcpetion;
import org.rapla.entities.storage.internal.SimpleEntity;
import org.rapla.facade.Conflict;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A conflict is the allocation of the same resource at the same time by different
 * reservations. There's one conflict for each resource and each overlapping of
 * two allocations. So if there are 3 reservations that allocate the same 2 resources
 * on 2 days of the week, then we got ( 3 * 2 ) *  2 * 2 = 24 conflicts. Thats
 * 3 reservations, each conflicting with two other 2 reservations on 2 days with 2 resources.
 *
 * @author Christopher Kohlhaas
 */

public class ConflictImpl extends SimpleEntity implements Conflict, ModifiableTimestamp
{
    private Date startDate;
    private Date lastChanged;
    private String reservation1Name;
    private String reservation2Name;
    private RepeatingType repeatingType1;
    private RepeatingType repeatingType2;
    private boolean appointment1Enabled = true;
    private boolean appointment2Enabled = true;
    private boolean appointment1Editable = true;
    private boolean appointment2Editable = true;

    ConflictImpl()
    {
    }

    public ConflictImpl(String id, Date today, Date lastChanged) throws RaplaException
    {
        startDate = today;
        this.lastChanged = lastChanged;
        reservation1Name = "";
        reservation2Name = "";
        setId(id);
        String[] split = splitConflictId(id);
        if (split == null)
        {
            throw new RaplaException(id + " is no conflict id");
        }
        String allocId = split[1];
        String app1Id = split[2];
        String app2Id = split[3];
        putId("allocatable", allocId);
        putId("appointment1", app1Id);
        putId("appointment2", app2Id);

    }

    /** Note that app1 does not necessarily go to appointment1 field.
     * The appointment with the lowest id goes to appointment1 and the other to appointment2*/
    public ConflictImpl(Allocatable allocatable, Appointment app1, Appointment app2, Date today)
    {
        this(allocatable, app1, app2, today, createId(allocatable.getReference(), app1.getReference(), app2.getReference()));
    }

    /** Note that app1 does not necessarily go to appointment1 field.
     * The appointment with the lowest id goes to appointment1 and the other to appointment2*/
    public ConflictImpl(Allocatable allocatable, Appointment app1, Appointment app2, Date today, String id)
    {
        lastChanged = getLastChanged(allocatable, app1, app2);
        putEntity("allocatable", allocatable);
        startDate = getStartDate_(today, app1, app2);
        if (app1.getId().compareTo(app2.getId()) >= 0)
        {
            Appointment temp = app1;
            app1 = app2;
            app2 = temp;
        }
        putEntity("appointment1", app1);
        putEntity("appointment2", app2);
        Reservation reservation1 = app1.getReservation();
        Reservation reservation2 = app2.getReservation();
        putEntity("reservation1", reservation1);
        putEntity("reservation2", reservation2);
        final ReferenceInfo<User> ownerRef1 = reservation1.getOwnerRef();
        putId("owner1", ownerRef1 != null ? ownerRef1.getId() : null);
        final ReferenceInfo<User> ownerRef2 = reservation2.getOwnerRef();
        putId("owner2", ownerRef2 != null ? ownerRef2.getId() : null);
        this.reservation1Name = getReservationName(reservation1, app1);
        final Repeating repeating1 = app1.getRepeating();
        this.repeatingType1 = repeating1 != null ? repeating1.getType() : null;
        final Repeating repeating2 = app2.getRepeating();
        this.repeatingType2 = repeating2 != null ? repeating2.getType() : null;
        this.reservation2Name = getReservationName(reservation2, app2);
        setResolver(((AllocatableImpl) allocatable).getResolver());
        setId(id);
    }

    private String getReservationName(Reservation reservation, Appointment appointment)
    {
        final String name = reservation.getName(Locale.getDefault());
        return name;
    }

    @Override
    public void setResolver(EntityResolver resolver)
    {
        super.setResolver(resolver);

        {
            Appointment appointment = resolver.tryResolve(getAppointment1());
            if (appointment != null)
            {
                Reservation reservation = appointment.getReservation();
                putId("reservation1", reservation.getId());
                reservation1Name = getReservationName(reservation, appointment);
                final Repeating repeating = appointment.getRepeating();
                this.repeatingType1 = repeating != null ? repeating.getType() : null;
            }
        }
        {
            Appointment appointment = resolver.tryResolve(getAppointment2());
            if (appointment != null)
            {
                Reservation reservation = appointment.getReservation();
                putId("reservation2", reservation.getId());
                reservation2Name = getReservationName(reservation, appointment);
                final Repeating repeating = appointment.getRepeating();
                this.repeatingType2 = repeating != null ? repeating.getType() : null;
            }
        }
    }

    static private String[] splitConflictId(String id)
    {
        String[] split = id.split(";");
        if (split.length < 5)
        {
            if (!split[0].equalsIgnoreCase("CONFLICT"))
            {
                return null;
            }
            return split;
        }
        else
        {
            return null;
        }
    }

    public Date getLastChanged()
    {
        return lastChanged;
    }

    public Date getCreateDate()
    {
        return lastChanged;
    }

    public void setLastChanged(Date date)
    {
        checkWritable();
        lastChanged = date;
    }

    @Override
    public void setCreateDate(Date date)
    {
        checkWritable();
        this.lastChanged = date;
    }

    @Override
    public String getReservation1Name()
    {
        return reservation1Name;
    }

    @Override
    public RepeatingType getRepeatingType1()
    {
        return repeatingType1;
    }

    @Override
    public RepeatingType getRepeatingType2()
    {
        return repeatingType2;
    }

    @Override
    public String getReservation2Name()
    {
        return reservation2Name;
    }

    @Override
    public Date getStartDate()
    {
        return startDate;
    }

    @Override public Iterable<ReferenceInfo> getReferenceInfo()
    {
        return Collections.emptyList();
    }

    private Date getStartDate_(Date today, Appointment app1, Appointment app2)
    {
        Date fromDate = today;
        Date start1 = app1.getStart();
        if (start1.before(fromDate))
        {
            fromDate = start1;
        }
        Date start2 = app2.getStart();
        if (start2.before(fromDate))
        {
            fromDate = start2;
        }
        Date toDate = DateTools.addDays(today, 365 * 10);
        Date date = getFirstConflictDate(fromDate, toDate, app1, app2);
        if (date == null)
        {
            // in case no overlapping is found, add the last of the start dates as conflict date
            // this should not occur often as conflicts should only be added if they have overlapping appointments
            // however noone prevents an api user from doing this
            date = start1.after(start2) ? start1 : start2;
        }
        return date;
    }

    static public String createId(ReferenceInfo<Allocatable> allocId, ReferenceInfo<Appointment> id1, ReferenceInfo<Appointment> id2)
    {
        StringBuilder buf = new StringBuilder();
        //String id1 = getId("appointment1");
        //String id2 = getId("appointment2");
        if (id1.equals(id2))
        {
            throw new IllegalStateException("ids of conflicting appointments are the same " + id1);
        }
        buf.append("CONFLICT;");
        buf.append(allocId.getId());
        buf.append(';');
        buf.append(id1.compareTo(id2) < 0 ? id1.getId() : id2.getId());
        buf.append(';');
        buf.append(id1.compareTo(id2) < 0 ? id2.getId() : id1.getId());
        return buf.toString();
    }

    //	public ConflictImpl(String id) throws RaplaException {
    //		String[] split = id.split(";");
    //		ReferenceHandler referenceHandler = getReferenceHandler();
    //		referenceHandler.putId("allocatable", LocalCache.getId(Allocatable.TYPE,split[0]));
    //		referenceHandler.putId("appointment1", LocalCache.getId(Appointment.TYPE,split[1]));
    //		referenceHandler.putId("appointment2", LocalCache.getId(Appointment.TYPE,split[2]));
    //		setId( id);
    //	}

    //	public static boolean isConflictId(String id) {
    //		if ( id == null)
    //		{
    //			return false;
    //		}
    //		String[] split = id.split(";");
    //		if ( split.length != 3)
    //		{
    //			return false;
    //		}
    //		try {
    //			LocalCache.getId(Allocatable.TYPE,split[0]);
    //			LocalCache.getId(Appointment.TYPE,split[1]);
    //			LocalCache.getId(Appointment.TYPE,split[2]);
    //		} catch (RaplaException e) {
    //			return false;
    //		}
    //		return true;
    //	}

    /** @return the first Reservation, that is involed in the conflict.*/
    //    public Reservation getReservation1()
    //    {
    //    	Appointment appointment1 = getAppointment1();
    //		if ( appointment1 == null)
    //		{
    //			throw new IllegalStateException("Appointment 1 is null resolve not called");
    //		}
    //    	return appointment1.getReservation();
    //    }

    /** The appointment of the first reservation, that causes the conflict. */
    public ReferenceInfo<Appointment> getAppointment1()
    {
        return getRef("appointment1", Appointment.class);
    }

    public ReferenceInfo<Appointment> getAppointment2()
    {
        return getRef("appointment2", Appointment.class);
    }

    public ReferenceInfo<Reservation> getReservation1()
    {
        return getRef("reservation1", Reservation.class);
    }

    public ReferenceInfo<Reservation> getReservation2()
    {
        return getRef("reservation2", Reservation.class);
    }

    /** @return the allocatable, allocated for the same time by two different reservations. */
    public Allocatable getAllocatable()
    {
        return getEntity("allocatable", Allocatable.class);
    }

    public ReferenceInfo<Allocatable> getAllocatableId()
    {
        return getRef("allocatable", Allocatable.class);
    }
    //    /** @return the second Reservation, that is involed in the conflict.*/
    //    public Reservation getReservation2()
    //    {
    //    	Appointment appointment2 = getAppointment2();
    //    	if ( appointment2 == null)
    //		{
    //			throw new IllegalStateException("Appointment 2 is null resolve not called");
    //		}
    //		return appointment2.getReservation();
    //    }
    //    /** @return The User, who created the second Reservation.*/
    //    public User getUser2()
    //    {
    //    	return getReservation2().getOwner();
    //    }

    /** The appointment of the second reservation, that causes the conflict. */


    public static final ConflictImpl[] CONFLICT_ARRAY = new ConflictImpl[] {};

    /**
     * @see org.rapla.entities.Named#getName(java.util.Locale)
     */
    public String getName(Locale locale)
    {
        return getAllocatable().getName(locale);
    }

    public boolean isOwner(User user)
    {
        if ( user == null)
        {
            return true;
        }
        String userId = user.getId();
        String owner1 = getOwner1();
        String owner2 = getOwner2();
        return userId.equals(owner1) || userId.equals(owner2);
    }

    // public Date getFirstConflictDate(final Date  fromDate, Date toDate) {
    // Appointment a1  =getAppointment1();
    // Appointment a2  =getAppointment2();
    // return getFirstConflictDate(fromDate, toDate, a1, a2);
    //}

    public String getOwner1()
    {
        return getId("owner1");
    }

    public String getOwner2()
    {
        return getId("owner2");
    }

    private boolean contains(ReferenceInfo<Appointment> appointmentId)
    {
        if (appointmentId == null)
            return false;

        ReferenceInfo<Appointment> app1 = getAppointment1();
        ReferenceInfo<Appointment> app2 = getAppointment2();
        if (app1 != null && app1.equals(appointmentId))
            return true;
        return app2 != null && app2.equals(appointmentId);
    }

    static public boolean equals(ConflictImpl firstConflict, Conflict secondConflict)
    {
        ReferenceInfo<Allocatable> allocatable = firstConflict.getAllocatableId();
        if (allocatable != null && !allocatable.equals(secondConflict.getAllocatableId()))
        {
            return false;
        }
        if (secondConflict == null)
            return false;
        if (!firstConflict.contains(secondConflict.getAppointment1()))
            return false;
        return firstConflict.contains(secondConflict.getAppointment2());

    }

    private static boolean contains(ConflictImpl conflict, Collection<Conflict> conflictList)
    {
        for (Conflict conf : conflictList)
        {
            if (equals(conflict, conf))
            {
                return true;
            }
        }
        return false;
    }

    @Override public Class<Conflict> getTypeClass()
    {
        return Conflict.class;
    }

    public String toString()
    {
        Conflict conflict = this;
        StringBuffer buf = new StringBuffer();
        buf.append("Conflict for ");
        try
        {
            final Allocatable allocatable = conflict.getAllocatable();
            buf.append(allocatable);
        }
        catch ( Throwable ex)
        {
            buf.append( conflict.getAllocatableId());
        }
        buf.append(" ");
        buf.append(conflict.getAppointment1());
        buf.append(" ");
        buf.append("with");
        buf.append(" ");
        buf.append(conflict.getAppointment2());
        buf.append(" ");
        return buf.toString();
    }

    static public Date getFirstConflictDate(final Date fromDate, Date toDate, Appointment a1, Appointment a2)
    {
        Date minEnd = a1.getMaxEnd();
        if (a1.getMaxEnd() != null && a2.getMaxEnd() != null && a2.getMaxEnd().before(a1.getMaxEnd()))
        {
            minEnd = a2.getMaxEnd();
        }
        Date maxStart = a1.getStart();
        if (a2.getStart().after(a1.getStart()))
        {
            maxStart = a2.getStart();
        }
        if (fromDate != null && maxStart.before(fromDate))
        {
            maxStart = fromDate;
        }
        // look for  10 years in the future (365 days * 10 days +3 for possible leap years)
        if (minEnd == null)
            minEnd = new Date(maxStart.getTime() + DateTools.MILLISECONDS_PER_DAY * (365 * 10 + 3));

        if (toDate != null && minEnd.after(toDate))
        {
            minEnd = toDate;
        }

        List<AppointmentBlock> listA = new ArrayList<>();
        a1.createBlocks(maxStart, minEnd, listA);
        List<AppointmentBlock> listB = new ArrayList<>();
        a2.createBlocks(maxStart, minEnd, listB);
        for (int i = 0, j = 0; i < listA.size() && j < listB.size(); )
        {
            long s1 = listA.get(i).getStart();
            long s2 = listB.get(j).getStart();
            long e1 = listA.get(i).getEnd();
            long e2 = listB.get(j).getEnd();
            if (s1 < e2 && s2 < e1)
            {
                return new Date(Math.max(s1, s2));
            }
            if (s1 > s2)
                j++;
            else
                i++;
        }
        return null;
    }

    public static void checkAndAddConflicts(Collection<Conflict> conflictList, Allocatable allocatable, Appointment appointment1, Appointment appointment2,
            Date today)
    {
        if (ConflictImpl.isConflict(appointment1, appointment2, today))
        {
            final ConflictImpl conflict = new ConflictImpl(allocatable, appointment1, appointment2, today);
            // Rapla 1.4: Don't add conflicts twice
            if (!contains(conflict, conflictList))
            {
                conflictList.add(conflict);
            }
        }
    }

    @Nullable public static Date getLastChanged(Allocatable allocatable, Appointment appointment1, Appointment appointment2)
    {
        Date lastChanged = allocatable.getLastChanged();
        final Reservation reservation1 = appointment1.getReservation();
        final Reservation reservation2 = appointment2.getReservation();
        final Date lastChanged1 = reservation1 != null ? reservation1.getLastChanged() : null;
        final Date lastChanged2 = reservation2 != null ? reservation2.getLastChanged() : null;
        if (lastChanged == null || (lastChanged1 != null && lastChanged1.after(lastChanged)))
        {
            lastChanged = lastChanged1;
        }
        if (lastChanged == null || (lastChanged2 != null && lastChanged2.after(lastChanged)))
        {
            lastChanged = lastChanged2;
        }
        return lastChanged;
    }

    public static boolean endsBefore(Appointment appointment1, Appointment appointment2, Date date)
    {
        Date maxEnd1 = appointment1.getMaxEnd();
        Date maxEnd2 = appointment2.getMaxEnd();
        if (maxEnd1 != null && maxEnd1.before(date))
        {
            return true;
        }
        return maxEnd2 != null && maxEnd2.before(date);
    }

    public static boolean isConflict(Appointment appointment1, Appointment appointment2, Date today)
    {
        // Don't add conflicts, when in the past
        if (endsBefore(appointment1, appointment2, today))
        {
            return false;
        }
        if (appointment1.equals(appointment2))
            return false;

        if (RaplaComponent.isTemplate(appointment1))
        {
            return false;
        }
        if (RaplaComponent.isTemplate(appointment2))
        {
            return false;
        }
        boolean conflictTypes = checkForConflictTypes(appointment1, appointment2);
        if (!conflictTypes)
        {
            return false;
        }
        Date maxEnd1 = appointment1.getMaxEnd();
        Date maxEnd2 = appointment2.getMaxEnd();
        Date checkEnd = maxEnd1;
        if (maxEnd2 != null && checkEnd != null && maxEnd2.before(checkEnd))
        {
            checkEnd = maxEnd2;
        }
        return ConflictImpl.getFirstConflictDate(today, checkEnd, appointment1, appointment2) != null;
    }

    public static boolean isConflictWithoutCheck(Appointment appointment1, Appointment appointment2, Date today)
    {
        // Don't add conflicts, when in the past
        if (appointment1.equals(appointment2))
            return false;

        boolean conflictTypes = checkForConflictTypes2(appointment1, appointment2);
        if (!conflictTypes)
        {
            return false;
        }
        Date maxEnd1 = appointment1.getMaxEnd();
        Date maxEnd2 = appointment2.getMaxEnd();
        Date checkEnd = maxEnd1;
        if (maxEnd2 != null && checkEnd != null && maxEnd2.before(checkEnd))
        {
            checkEnd = maxEnd2;
        }
        return !(checkEnd != null && ConflictImpl.getFirstConflictDate(today, checkEnd, appointment1, appointment2) == null);
    }

    @SuppressWarnings("null") private static boolean checkForConflictTypes(Appointment a1, Appointment a2)
    {
        Reservation r1 = a1.getReservation();
        DynamicType type1 = r1 != null ? r1.getClassification().getType() : null;
        String annotation1 = getConflictAnnotation(type1);
        if (isNoConflicts(annotation1))
        {
            return false;
        }
        Reservation r2 = a2.getReservation();
        DynamicType type2 = r2 != null ? r2.getClassification().getType() : null;
        String annotation2 = getConflictAnnotation(type2);
        if (isNoConflicts(annotation2))
        {
            return false;
        }
        if (annotation1 != null)
        {
            if (annotation1.equals(DynamicTypeAnnotations.VALUE_CONFLICTS_WITH_OTHER_TYPES))
            {
                if (type2 != null)
                {
                    return !type1.equals(type2);
                }
            }
        }
        return true;
    }

    @SuppressWarnings("null") private static boolean checkForConflictTypes2(Appointment a1, Appointment a2)
    {
        Reservation r1 = a1.getReservation();
        DynamicType type1 = r1 != null ? r1.getClassification().getType() : null;
        String annotation1 = getConflictAnnotation(type1);
        Reservation r2 = a2.getReservation();
        DynamicType type2 = r2 != null ? r2.getClassification().getType() : null;
        if (annotation1 != null)
        {
            if (annotation1.equals(DynamicTypeAnnotations.VALUE_CONFLICTS_WITH_OTHER_TYPES))
            {
                if (type2 != null)
                {
                    return !type1.equals(type2);
                }
            }
        }
        return true;
    }

    public static boolean isNoConflicts(String annotation)
    {
        return annotation != null && annotation.equals(DynamicTypeAnnotations.VALUE_CONFLICTS_NONE);
    }

    public static String getConflictAnnotation(DynamicType type)
    {
        if (type == null)
        {
            return null;
        }
        return type.getAnnotation(DynamicTypeAnnotations.KEY_CONFLICTS);
    }

    public boolean hasAppointment(Appointment appointment)
    {
        boolean result = getAppointment1().equals(appointment) || getAppointment2().equals(appointment);
        return result;
    }

    @Override public Conflict clone()
    {
        ConflictImpl clone = new ConflictImpl();
        super.deepClone(clone);
        clone.appointment1Enabled = appointment1Enabled;
        clone.appointment2Enabled = appointment2Enabled;
        clone.appointment1Editable = appointment1Editable;
        clone.appointment2Editable = appointment2Editable;
        clone.reservation1Name = reservation1Name;
        clone.reservation2Name = reservation2Name;
        clone.startDate = startDate;
        clone.repeatingType1 = repeatingType1;
        clone.repeatingType2 = repeatingType2;
        clone.lastChanged = lastChanged;
        return clone;
    }

    public boolean isAppointment1Enabled()
    {
        return appointment1Enabled;
    }

    public void setAppointment1Enabled(boolean appointment1enabled)
    {
        this.appointment1Enabled = appointment1enabled;
    }

    public boolean isAppointment2Enabled()
    {
        return appointment2Enabled;
    }

    public void setAppointment2Enabled(boolean appointment2enabled)
    {
        this.appointment2Enabled = appointment2enabled;
    }

    public boolean isAppointment1Editable()
    {
        return appointment1Editable;
    }

    public void setAppointment1Editable(boolean appointment1Editable)
    {
        this.appointment1Editable = appointment1Editable;
    }

    public boolean isAppointment2Editable()
    {
        return appointment2Editable;
    }

    public void setAppointment2Editable(boolean appointment2Editable)
    {
        this.appointment2Editable = appointment2Editable;
    }

    public static Map<Appointment, Set<Appointment>> getMap(Collection<Conflict> selectedConflicts, Collection<Appointment> appointments)
    {
        Map<Appointment, Set<Appointment>> result = new HashMap<>();
        Map<ReferenceInfo<Appointment>, Appointment> map = new HashMap<>();
        for (Appointment app : appointments)
        {
            map.put(app.getReference(), app);
        }
        for (Conflict conflict : selectedConflicts)
        {
            Appointment app1 = map.get(conflict.getAppointment1());
            Appointment app2 = map.get(conflict.getAppointment2());
            add(result, app1, app2);
            add(result, app2, app1);
        }
        return result;
    }

    private static void add(Map<Appointment, Set<Appointment>> result, Appointment app1, Appointment app2)
    {
        if ( app1 == null || app2 == null)
        {
            return;
        }
        Set<Appointment> set = result.get(app1);
        if (set == null)
        {
            set = new HashSet<>();
            result.put(app1, set);
        }
        set.add(app2);
    }

    public boolean checkEnabled()
    {
        boolean appointment1Enabled = isAppointment1Enabled();
        boolean appointment2Enabled = isAppointment2Enabled();
        boolean appointment1Editable = isAppointment1Editable();
        boolean appointment2Editable = isAppointment2Editable();
        // only one editable and enabled appointment is enough to show the conflict as enabled
        // if the user has both editableflags and disabled the conflict by itself, both should be disabled
        if (appointment1Enabled && appointment1Editable)
        {
            return true;
        }
        return appointment2Enabled && appointment2Editable;
    }
}










