/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import javax.annotation.Nonnull;

import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.domain.user.ISMPUserManager;
import com.helger.phoss.smp.settings.ISMPSettingsManager;

/**
 * This {@link ISMPManagerProvider} implementation returns non-<code>null</code>
 * managers that all do nothing. This is only needed to access the identifier
 * factory.<br>
 * Note: this class must be public
 *
 * @author Philip Helger
 */
public final class MockSMPManagerProvider implements ISMPManagerProvider
{
  @Nonnull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    return new MockSMLInfoManager ();
  }

  @Nonnull
  public ISMPSettingsManager createSettingsMgr ()
  {
    return new MockSMPSettingsManager ();
  }

  @Nonnull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    return new MockSMPTransportProfileManager ();
  }

  @Nonnull
  public ISMPUserManager createUserMgr ()
  {
    return new MockSMPUserManager ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new MockSMPServiceGroupManager ();
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    return new MockSMPRedirectManager ();
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    return new MockSMPServiceInformationManager ();
  }

  @Nonnull
  public ISMPBusinessCardManager createBusinessCardMgr (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new MockSMPBusinessCardManager ();
  }
}