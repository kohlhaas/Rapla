/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Gereon Fassbender, Christopher Kohlhaas               |
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
package org.rapla.facade;

import io.reactivex.rxjava3.functions.Consumer;
import org.rapla.components.util.TimeInterval;
import org.rapla.entities.Category;
import org.rapla.entities.Entity;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaMap;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.ReferenceInfo;
import org.rapla.framework.RaplaException;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;
import org.rapla.storage.PermissionController;
import org.rapla.storage.StorageOperator;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/** A collection of all module-interfaces
*/
public interface RaplaFacade
{
    CommandScheduler getScheduler();
    /** Methods for quering the various entities of the backend
     */
    StorageOperator getOperator();
    PermissionController getPermissionController();

    <T extends Entity> T tryResolve( ReferenceInfo<T> info);

    <T extends Entity> T resolve( ReferenceInfo<T> info) throws EntityNotFoundException;

    /** returns all DynamicTypes matching the specified classification
     possible keys are reservation, person and resource.
     @see org.rapla.entities.dynamictype.DynamicTypeAnnotations
     */
    DynamicType[] getDynamicTypes(String classificationType) throws RaplaException;

    /** returns the DynamicType with the passed elementKey */
    DynamicType getDynamicType(String elementKey) throws RaplaException;

    /** returns The root category.   */
    Category getSuperCategory();

    /** returns The category that contains all the user-groups of rapla   */
    Category getUserGroupsCategory() throws RaplaException;

    /** returns all users  */
    User[] getUsers() throws RaplaException;

    /** returns the user with the specified username */
    User getUser(String username) throws RaplaException;

    /** returns all allocatables that match the passed ClassificationFilter. If null all readable allocatables are returned*/
    Allocatable[] getAllocatablesWithFilter(ClassificationFilter[] filters) throws RaplaException;

    /** returns all readable allocatables, same as getAllocatables(null)*/
    Allocatable[] getAllocatables() throws RaplaException;

    /** returns the reservations of the specified user in the specified interval
     @param user  A user-object or null for all users
     @param start only reservations beginning after the start-date will be returned (can be null).
     @param end   only reservations beginning before the end-date will be returned (can be null).
     @param reservationFilters  you can specify classificationfilters or null for all reservations .
     */
    Promise<Collection<Reservation>> getReservations(User user,Date start,Date end,ClassificationFilter[] reservationFilters);
    Promise<Collection<Reservation>> getReservationsAsync(User user, Allocatable[] allocatables, User[] owners, Date start, Date end, ClassificationFilter[] reservationFilters);




    /**returns all reservations that have allocated at least one Resource or Person that is part of the allocatables array.
     @param allocatables only reservations that allocate at least on element of this array will be returned.
     @param start only reservations beginning after the start-date will be returned (can be null).
     @param end   only reservations beginning before the end-date will be returned (can be null).
     @param filters  you can specify classificationfilters or null for all reservations.
     **/
    Promise<Collection<Reservation>> getReservationsForAllocatable(Allocatable[] allocatables, Date start,Date end,ClassificationFilter[] filters);

    /** returns the current date in GMT+0 Timezone. If rapla operates
     in multi-user mode, the date should be calculated from the
     server date.
     */
    Date today();

    /** returns all allocatables from the set of passed allocatables, that are already allocated by different parallel reservations at the time-slices, that are described by the appointment */
    Promise<Map<ReferenceInfo<Allocatable>, Collection<Appointment>>> getAllocatableBindings(Collection<Allocatable> allocatables, Collection<Appointment> forAppointment);

    /** returns all existing conflicts with the reservation */
    Promise<Collection<Conflict>> getConflictsForReservation(Reservation reservation);


    /** returns if the user has the permissions to change/createInfoDialog an
     allocation on the passed appointment. Changes of an
     existing appointment that are in an permisable
     timeframe are allowed. Example: The extension of an exisiting appointment,
     doesn't affect allocations in the past and should not createInfoDialog a
     conflict with the permissions.
     */
    //boolean hasPermissionToAllocate( Appointment appointment, Allocatable allocatable );

    /** returns the preferences for the passed user, must be admin todo this. creates a new prefence object if not set*/
    Preferences getPreferences(User user) throws RaplaException;

    Preferences getSystemPreferences() throws RaplaException;

    Preferences getAdminPreferences() throws RaplaException;

    /** returns if the user is allowed to exchange the allocatables of this reservation. A user can do it if he has
     * at least admin privileges for one allocatable. He can only exchange or remove or insert allocatables he has admin privileges on.
     * The User cannot change appointments.*/
    boolean canExchangeAllocatables(User user,Reservation reservation);

    Collection<Allocatable> getTemplates() throws RaplaException;

    Promise<Collection<Reservation>> getTemplateReservations(Allocatable name);

    Promise<Date> getNextAllocatableDate(Collection<Allocatable> asList, Appointment appointment, CalendarOptions options);

    boolean canAllocate(CalendarModel model,User user);

    /** All methods that allow modifing the entity-objects.
     */

    /** Creates a new event,  Creates a new event from the first dynamic type found, basically a shortcut to newReservation(getDynamicType(VALUE_CLASSIFICATION_TYPE_RESERVATION)[0].newClassification())
     * This is a convenience method for testing.
     */
    @Deprecated Reservation newReservationDeprecated() throws RaplaException;

    /** Creates a new resource from the first dynamic type found, basically a shortcut to newAlloctable(getDynamicType(VALUE_CLASSIFICATION_TYPE_RESOURCE)[0].newClassification()).
     * This is a convenience method for testing.
     *  */
    @Deprecated Allocatable newResourceDeprecated() throws RaplaException;

    /** creates a new Rapla Map. Keep in mind that only RaplaObjects and Strings are allowed as entries for a RaplaMap!*/
    <T> RaplaMap<T> newRaplaMapForMap( Map<String,T> map);
    /** creates an ordered RaplaMap with the entries of the collection as values and their position in the collection from 1..n as keys*/
    <T> RaplaMap<T> newRaplaMap( Collection<T> col);

    CalendarSelectionModel newCalendarModel( User user) throws RaplaException;

    /** Creates a new reservation from the classifcation object and with the passed user as its owner
     * You can createInfoDialog a new classification from a {@link DynamicType} with newClassification method.
     * @see DynamicType#newClassification()
     */
    Reservation newReservation(Classification classification,User user) throws RaplaException;

    /** @deprecated use #newAppointmentWithUser or #newAppointmentAsync (on the client) instead */
    @Deprecated
    Appointment newAppointmentDeprecated(Date startDate, Date endDate) throws RaplaException;

    Promise<Reservation> newReservationAsync(Classification classification);
    Promise<Appointment> newAppointmentAsync(TimeInterval interval);
    Promise<Collection<Appointment>> newAppointmentsAsync(Collection<TimeInterval> interval);
    Appointment newAppointmentWithUser(Date startDate,Date endDate, User user) throws RaplaException;

    /** Creates a new allocatable from the classifcation object and with the passed user as its owner
     * You can createInfoDialog a new classification from a {@link DynamicType} with newClassification method.
     * @see DynamicType#newClassification()*/
    Allocatable newAllocatable( Classification classification, User user) throws RaplaException;
    Allocatable newPeriod(User user) throws RaplaException;

    Category newCategory() throws RaplaException;

    /**
     * @param classificationType @see DynamicTypeAnnotations
     * @return
     * @throws RaplaException
     */
    DynamicType newDynamicType(String classificationType) throws RaplaException;
    Attribute newAttribute(AttributeType attributeType) throws RaplaException;
    User newUser() throws RaplaException;

    /** Clones an entity. The entities will get new identifier and
     won't be equal to the original. The resulting object is not persistent and therefore
     can be editet.
     */
    <T extends Entity> T clone(T obj,User user) throws RaplaException;
    <T extends Entity> Promise<T> cloneAsync(T obj);
    <T extends Entity> Promise<Collection<T>> cloneList(Collection<T> obj);
    <T extends Entity> Promise<Map<T,T>> editListAsync(Collection<T> obj);
    <T extends Entity> Promise<Map<T,T>> editListAsyncForUndo(Collection<T> obj);
    <T extends Entity> Promise<T> editAsync(T obj);
    <T extends Entity> Promise<Void> update(T entity, Consumer<T> updateFunction);
    <T extends Entity> Promise<Void> updateList(Collection<T> list, Consumer<Collection<T>> updateFunction);

    /** copies a list of reservations to a new beginning. KeepTime specifies if the original time is used or the time of the new beginDate*/
    Promise<Collection<Reservation>> copyReservations(Collection<Reservation> toCopy, Date beginn, boolean keepTime, User user);

    <T extends Entity, S extends Entity> Promise<Void> dispatch( Collection<T> storeList, Collection<ReferenceInfo<S>> removeList);

    <T extends Entity<T>> Promise<Void> dispatchRemove(Collection<ReferenceInfo<T>> toRemove, boolean forceRessourceDelete);

    /**
     * Does a merge of allocatables. A merge is defined as the given object will be stored if writeable and then
     * all references to the provided allocatableIds are replaced with the selected allocatable. Afterwards the
     * allocatables with the given allocatableIds are deleted.
     *
     * @param selectedObject
     *              the winning allocatable, which will replace all references of the allocatableIds
     * @param allocatableIds
     *              the ids for the allocatables to merge into the selectedObject
     */
    Promise<Allocatable> doMerge(Allocatable selectedObject, Set<ReferenceInfo<Allocatable>> allocatableIds, User user);

    <T extends Entity> Collection<T> editList(Collection<T> list) throws RaplaException;

    /** This call will be delegated to the {@link org.rapla.storage.StorageOperator}. It
     * returns an editable working copyReservations of an object. Only objects return by this method and new objects are editable.
     * To get the persistent, non-editable version of a working copyReservations use {@link #getPersistant} */
    <T extends Entity> T edit(T obj) throws RaplaException;

    /** Returns the persistant version of a working copyReservations.
     * Throws an {@link org.rapla.entities.EntityNotFoundException} when the
     * object is not found
     * @see #edit
     * @see #clone
     */
    <T extends Entity> T getPersistent(T working) throws RaplaException;

    <T extends Entity> Map<T,T> getPersistentForList(Collection<T> list) throws RaplaException;

    /** This call will be delegated to the {@link org.rapla.storage.StorageOperator} */
    <T extends Entity> void storeObjects(T[] obj) throws RaplaException;
    /** @see #storeObjects(Entity[]) */
    <T extends Entity> void store(T obj) throws RaplaException;

    /** This call will be delegated to the {@link org.rapla.storage.StorageOperator} */
    <T extends Entity> void removeObjects(T[] obj) throws RaplaException;

    /** @see #removeObjects(Entity[]) */
    <T extends Entity> void remove(T obj) throws RaplaException;

    /** stores and removes objects in the one transaction
     * @throws RaplaException */
    <T extends Entity, S extends Entity> void storeAndRemove( T[] storedObjects, S[] removedObjects, User user) throws RaplaException;




    /**
     *  Refreshes the data that is in the cache (or on the client)
     and notifies all registered {@link ModificationListener ModificationListeners}
     with an update-event.
     There are two types of refreshs.

     <ul>
     <li>Incremental Refresh: Only the changes are propagated</li>
     <li>Full Refresh: The complete data is reread. (Currently disabled in Rapla)</li>
     </ul>

     <p>
     Incremental refreshs are the normal case if you have a client server basis.
     (In a single user system no refreshs are necessary at all).
     The refreshs are triggered in defined intervals if you use the webbased communication
     and automaticaly if you use the old communication layer. You can change the refresh interval
     via the admin options.
     </p>
     <p>
     Of course you can call a refresh anytime you want to synchronize with the server, e.g. if
     you want to ensure you are uptodate before editing. If you are on the server you dont need to refresh.
     </p>


     <strong>WARNING: When using full refresh on a local file storage
     all information will be  changed. So use it
     only if you modify the data from external.
     You better re-get and re-draw all
     the information in the Frontend after a full refresh.
     </strong>
     */
    void refresh() throws RaplaException;

    Promise<Void> refreshAsync();

    /** returns all existing conflicts that are visible for the user
     conflicts
     */
    Promise<Collection<Conflict>> getConflicts();

    Promise<Collection<Reservation>> getResourceRequests();

    /** returns all available periods */
    Period[] getPeriods() throws RaplaException;

    /** returns an Interface for accessing the periods
     * @throws RaplaException */
    PeriodModel getPeriodModel() throws RaplaException;

    /** returns an Interface for accessing the periods
     * @throws RaplaException */
    PeriodModel getPeriodModelFor(String key) throws RaplaException;

    Promise<Void> moveCategory(Category categoryToMove, Category targetCategory);


    enum ChangeState
    {
        latest,newerVersionAvailable,deleted
    }
    Promise<ChangeState> getUpdateState(Entity original);
}





