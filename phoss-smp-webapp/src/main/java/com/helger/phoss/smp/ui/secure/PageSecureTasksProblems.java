/**
 * Copyright (C) 2014-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.ui.secure;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.SimpleURL;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.client.PDClientConfiguration;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.security.SMPTrustManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.uicore.css.CUICoreCSS;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.keystore.EKeyStoreLoadError;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;

public class PageSecureTasksProblems extends AbstractSMPWebPage
{
  public PageSecureTasksProblems (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Tasks/problems");
  }

  @Nonnull
  private IHCNode _createInfo (@Nonnull final String sMsg)
  {
    return badgeInfo ("Information: " + sMsg);
  }

  @Nonnull
  private IHCNode _createWarning (@Nonnull final String sMsg)
  {
    return badgeWarn ("Warning: " + sMsg);
  }

  @Nonnull
  private IHCNode _createError (@Nonnull final String sMsg)
  {
    return badgeDanger ("Error: " + sMsg);
  }

  private void _checkSettings (@Nonnull final HCOL aOL)
  {
    // Check that public URL is set
    if (StringHelper.hasNoText (SMPServerConfiguration.getPublicServerURL ()))
    {
      aOL.addItem (_createWarning ("The public server URL is not configured"),
                   div ("The configuration file property ").addChild (code (SMPServerConfiguration.KEY_SMP_PUBLIC_URL))
                                                           .addChild (" in file " +
                                                                      SMPServerConfiguration.PATH_SMP_SERVER_PROPERTIES +
                                                                      " is not set. This property is usually required to create valid Internet-URLs."));
    }

    // Check that the global debug setting is off
    if (GlobalDebug.isDebugMode ())
    {
      aOL.addItem (_createWarning ("Debug mode is enabled."),
                   div ("This mode enables increased debug checks and logging and therefore results in reduced performance.").addChild (" This should not be enabled in a production system.")
                                                                                                                             .addChild (" The configuration file property ")
                                                                                                                             .addChild (code (SMPWebAppConfiguration.WEBAPP_KEY_GLOBAL_DEBUG))
                                                                                                                             .addChild (" in file " +
                                                                                                                                        SMPWebAppConfiguration.PATH_WEBAPP_PROPERTIES +
                                                                                                                                        " should be set to ")
                                                                                                                             .addChild (code ("false"))
                                                                                                                             .addChild (" to fix this."));
    }

    // Check that the global production mode is on
    if (!GlobalDebug.isProductionMode ())
    {
      aOL.addItem (_createWarning ("Production mode is disabled."),
                   div ("This mode is required so that all background jobs are enabled and mail sending works (if configured).").addChild (" This should be enabled in a production system.")
                                                                                                                                .addChild (" The configuration file property ")
                                                                                                                                .addChild (code (SMPWebAppConfiguration.WEBAPP_KEY_GLOBAL_PRODUCTION))
                                                                                                                                .addChild (" in file " +
                                                                                                                                           SMPWebAppConfiguration.PATH_WEBAPP_PROPERTIES +
                                                                                                                                           " should be set to ")
                                                                                                                                .addChild (code ("true"))
                                                                                                                                .addChild (" to fix this."));
    }
  }

  private void _checkPrivateKey (@Nonnull final WebPageExecutionContext aWPEC,
                                 @Nonnull final HCOL aOL,
                                 @Nonnull final LocalDateTime aNowDT,
                                 @Nonnull final LocalDateTime aNowPlusDT,
                                 @Nonnull final KeyStore.PrivateKeyEntry aKeyEntry)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final Certificate [] aChain = aKeyEntry.getCertificateChain ();
    for (final Certificate aCert : aChain)
    {
      if (aCert instanceof X509Certificate)
      {
        final X509Certificate aX509Cert = (X509Certificate) aCert;

        final String sLogPrefix = "The provided certificate with subject '" + aX509Cert.getSubjectX500Principal ().getName () + "' ";

        final LocalDateTime aNotBefore = PDTFactory.createLocalDateTime (aX509Cert.getNotBefore ());
        if (aNowDT.isBefore (aNotBefore))
        {
          aOL.addItem (_createError (sLogPrefix + " is not yet valid."),
                       div ("It will be valid from " + PDTToString.getAsString (aNotBefore, aDisplayLocale)));
        }

        final LocalDateTime aNotAfter = PDTFactory.createLocalDateTime (aX509Cert.getNotAfter ());
        if (aNowDT.isAfter (aNotAfter))
        {
          aOL.addItem (_createError (sLogPrefix + " is already expired."),
                       div ("It was valid until " + PDTToString.getAsString (aNotAfter, aDisplayLocale)));
        }
        else
          if (aNowPlusDT.isAfter (aNotAfter))
            aOL.addItem (_createWarning (sLogPrefix + " will expire soon."),
                         div ("It is only valid until " + PDTToString.getAsString (aNotAfter, aDisplayLocale)));
      }
      else
      {
        aOL.addItem (_createError ("At least one of the certificates is not an X.509 certificate! It is internally a " +
                                   ClassHelper.getClassName (aCert)));
        break;
      }
    }
  }

  private void _checkKeyStore (@Nonnull final WebPageExecutionContext aWPEC,
                               @Nonnull final HCOL aOL,
                               @Nonnull final LocalDateTime aNowDT,
                               @Nonnull final LocalDateTime aNowPlusDT)
  {
    if (!SMPKeyManager.isKeyStoreValid ())
    {
      // Loading failed - wrong path or wrong password or so
      aOL.addItem (_createError ("Problem with the certificate configuration"), div (SMPKeyManager.getInitializationError ()));
    }
    else
    {
      final KeyStore.PrivateKeyEntry aKeyEntry = SMPKeyManager.getInstance ().getPrivateKeyEntry ();
      if (aKeyEntry != null)
      {
        _checkPrivateKey (aWPEC, aOL, aNowDT, aNowPlusDT, aKeyEntry);
      }
      else
      {
        aOL.addItem (_createError ("Failed to load the configured private key from the keystore."),
                     div (SMPKeyManager.getInitializationError ()));
      }
    }
  }

  private void _iterateTrustStore (@Nonnull final WebPageExecutionContext aWPEC,
                                   @Nonnull final HCOL aOL,
                                   @Nonnull final LocalDateTime aNowDT,
                                   @Nonnull final LocalDateTime aNowPlusDT,
                                   @Nonnull final KeyStore aTrustStore)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    final HCOL aTrustStoreOL = new HCOL ();
    boolean bHasError = false;
    try
    {
      for (final String sAlias : CollectionHelper.newList (aTrustStore.aliases ()))
      {
        final Certificate aCert = aTrustStore.getCertificate (sAlias);
        if (aCert instanceof X509Certificate)
        {
          final X509Certificate aX509Cert = (X509Certificate) aCert;

          final String sLogPrefix = "The provided certificate with subject '" + aX509Cert.getSubjectX500Principal ().getName () + "' ";

          final LocalDateTime aNotBefore = PDTFactory.createLocalDateTime (aX509Cert.getNotBefore ());
          if (aNowDT.isBefore (aNotBefore))
          {
            aTrustStoreOL.addItem (_createError (sLogPrefix + " is not yet valid."),
                                   div ("It will be valid from " + PDTToString.getAsString (aNotBefore, aDisplayLocale)));
            bHasError = true;
          }

          final LocalDateTime aNotAfter = PDTFactory.createLocalDateTime (aX509Cert.getNotAfter ());
          if (aNowDT.isAfter (aNotAfter))
          {
            aTrustStoreOL.addItem (_createError (sLogPrefix + " is already expired."),
                                   div ("It was valid until " + PDTToString.getAsString (aNotAfter, aDisplayLocale)));
            bHasError = true;
          }
          else
            if (aNowPlusDT.isAfter (aNotAfter))
              aTrustStoreOL.addItem (_createWarning (sLogPrefix + " will expire soon."),
                                     div ("It is only valid until " + PDTToString.getAsString (aNotAfter, aDisplayLocale)));
        }
        else
          aTrustStoreOL.addItem (_createWarning ("The certificate is not an X.509 certificate! It is internally a " +
                                                 ClassHelper.getClassName (aCert)));
      }
    }
    catch (final GeneralSecurityException ex)
    {
      aTrustStoreOL.addItem (_createError ("Error iterating trust store."), div (SMPCommonUI.getTechnicalDetailsUI (ex)));
      bHasError = true;
    }

    if (aTrustStoreOL.hasChildren ())
      aOL.addItem (bHasError ? _createError ("Trust store issues") : _createWarning ("Trust store issues"), aTrustStoreOL);
  }

  private void _checkTrustStore (@Nonnull final WebPageExecutionContext aWPEC,
                                 @Nonnull final HCOL aOL,
                                 @Nonnull final LocalDateTime aNowDT,
                                 @Nonnull final LocalDateTime aNowPlusDT)
  {
    // check truststore configuration
    if (!SMPTrustManager.isTrustStoreValid ())
    {
      // Ignore error if no trust store is configured
      if (SMPTrustManager.getInitializationErrorCode () != EKeyStoreLoadError.KEYSTORE_NO_PATH)
        aOL.addItem (_createWarning ("Problem with the truststore configuration"), div (SMPTrustManager.getInitializationError ()));
    }
    else
    {
      // Successfully loaded trust store
      final KeyStore aTrustStore = SMPTrustManager.getInstance ().getTrustStore ();

      _iterateTrustStore (aWPEC, aOL, aNowDT, aNowPlusDT, aTrustStore);
    }
  }

  private void _checkSMLConfiguration (@Nonnull final HCOL aOL)
  {
    final ISMPSettings aSMPSettings = SMPMetaManager.getSettings ();
    final String sSMPID = SMPServerConfiguration.getSMLSMPID ();

    if (aSMPSettings.isSMLEnabled ())
    {
      final ISMLInfo aSMLInfo = aSMPSettings.getSMLInfo ();
      if (aSMLInfo == null)
      {
        aOL.addItem (_createError ("No SML is selected in the SMP settings."),
                     div ("All creations and deletions of service groups needs to be repeated when the SML connection is active!"));
      }
      else
      {
        // Check if this SMP is already registered
        final String sPublisherDNSName = sSMPID + "." + aSMLInfo.getPublisherDNSZone ();
        try
        {
          InetAddress.getByName (sPublisherDNSName);
          // On success, ignore
        }
        catch (final UnknownHostException ex)
        {
          // continue
          aOL.addItem (_createWarning ("It seems like this SMP was not yet registered to the SMP."),
                       div ("This is a one-time action that should be performed once. It requires a valid SMP certificate to work."),
                       div ("The registration check was performed with the URL ").addChild (new HCA ().setHref (new SimpleURL ("http://" +
                                                                                                                               sPublisherDNSName))
                                                                                                      .setTargetBlank ()
                                                                                                      .addChild (code (sPublisherDNSName))));
        }
      }
    }
    else
    {
      if (aSMPSettings.isSMLRequired ())
        aOL.addItem (_createError ("The connection to the SML is not enabled."),
                     div ("All creations and deletions of service groups needs to be repeated when the SML connection is active!"));
    }
  }

  private void _checkDirectoryConfig (@Nonnull final WebPageExecutionContext aWPEC,
                                      @Nonnull final HCOL aOL,
                                      @Nonnull final LocalDateTime aNowDT,
                                      @Nonnull final LocalDateTime aNowPlusDT)
  {
    final ISMPSettings aSMPSettings = SMPMetaManager.getSettings ();
    final String sDirectoryName = SMPWebAppConfiguration.getDirectoryName ();

    if (aSMPSettings.isDirectoryIntegrationEnabled ())
    {
      if (StringHelper.hasNoText (aSMPSettings.getDirectoryHostName ()))
        aOL.addItem (_createError ("An empty " + sDirectoryName + " hostname is provided"),
                     div ("A connection to the " + sDirectoryName + " server cannot be establised!"));

      // Check key store
      final LoadedKeyStore aLoadedKeyStore = PDClientConfiguration.loadKeyStore ();
      if (aLoadedKeyStore.isFailure ())
      {
        aOL.addItem (_createError ("The " + sDirectoryName + " client key store configuration is invalid."),
                     div (PeppolKeyStoreHelper.getLoadError (aLoadedKeyStore)));
      }
      else
      {
        final KeyStore aKeyStore = aLoadedKeyStore.getKeyStore ();
        final LoadedKey <KeyStore.PrivateKeyEntry> aLoadedKey = PDClientConfiguration.loadPrivateKey (aKeyStore);
        if (aLoadedKey.isFailure ())
        {
          aOL.addItem (_createError ("The " +
                                     sDirectoryName +
                                     " client key store could be read, but the private key configuration is invalid."),
                       div (PeppolKeyStoreHelper.getLoadError (aLoadedKey)));
        }
        else
        {
          _checkPrivateKey (aWPEC, aOL, aNowDT, aNowPlusDT, aLoadedKey.getKeyEntry ());
        }
      }

      // Check trust store
      final LoadedKeyStore aLoadedTrustStore = PDClientConfiguration.loadTrustStore ();
      if (aLoadedTrustStore.isFailure ())
      {
        aOL.addItem (_createError ("The " + sDirectoryName + " client trust store configuration is invalid."),
                     div (PeppolKeyStoreHelper.getLoadError (aLoadedTrustStore)));
      }
      else
      {
        final KeyStore aTrustStore = aLoadedTrustStore.getKeyStore ();
        _iterateTrustStore (aWPEC, aOL, aNowDT, aNowPlusDT, aTrustStore);
      }
    }
    else
    {
      // Warn only if Directory is required
      if (aSMPSettings.isDirectoryIntegrationRequired ())
        aOL.addItem (_createError ("The connection to " + sDirectoryName + " is not enabled."));
    }
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final LocalDateTime aNowDT = PDTFactory.getCurrentLocalDateTime ();
    final LocalDateTime aNowPlusDT = aNowDT.plusMonths (3);

    aNodeList.addChild (info ("This page tries to identify upcoming tasks and potential problems in the SMP configuration. It is meant to highlight immediate and upcoming action items as well as potential misconfiguration."));

    final HCOL aOL = new HCOL ();

    // Check for default password
    if (PhotonSecurityManager.getUserMgr ()
                             .areUserIDAndPasswordValid (CSecurity.USER_ADMINISTRATOR_ID, CSecurity.USER_ADMINISTRATOR_PASSWORD))
    {
      aOL.addItem (_createError ("Please change the password of the default user " + CSecurity.USER_ADMINISTRATOR_EMAIL + "!"),
                   div ("This is a severe security risk"));
    }

    _checkSettings (aOL);

    // check keystore configuration
    _checkKeyStore (aWPEC, aOL, aNowDT, aNowPlusDT);

    // Check truststore configuration
    _checkTrustStore (aWPEC, aOL, aNowDT, aNowPlusDT);

    // Check SML configuration
    _checkSMLConfiguration (aOL);

    // Check Directory configuration
    _checkDirectoryConfig (aWPEC, aOL, aNowDT, aNowPlusDT);

    // check service groups and redirects
    {
      final ICommonsList <ISMPServiceGroup> aServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();
      if (aServiceGroups.isEmpty ())
      {
        aOL.addItem (_createWarning ("No service group is configured. This SMP is currently empty."));
      }
      else
      {
        // For all service groups
        for (final ISMPServiceGroup aServiceGroup : CollectionHelper.getSorted (aServiceGroups, ISMPServiceGroup.comparator ()))
        {
          final HCUL aULPerSG = new HCUL ();
          final ICommonsList <ISMPServiceInformation> aServiceInfos = aServiceInfoMgr.getAllSMPServiceInformationOfServiceGroup (aServiceGroup);
          if (aServiceInfos.isEmpty ())
          {
            // This is merely a warning or an error
            aULPerSG.addItem (_createInfo ("No endpoint is configured for this service group."));
          }
          else
          {
            for (final ISMPServiceInformation aServiceInfo : aServiceInfos)
            {
              final HCUL aULPerDocType = new HCUL ();
              final ICommonsList <ISMPProcess> aProcesses = aServiceInfo.getAllProcesses ();
              for (final ISMPProcess aProcess : aProcesses)
              {
                final HCUL aULPerProcess = new HCUL ();
                final ICommonsList <ISMPEndpoint> aEndpoints = aProcess.getAllEndpoints ();
                for (final ISMPEndpoint aEndpoint : aEndpoints)
                {
                  final HCUL aULPerEndpoint = new HCUL ();

                  final ESMPTransportProfile eTransportProfile = ESMPTransportProfile.getFromIDOrNull (aEndpoint.getTransportProfile ());
                  if (eTransportProfile == null)
                    aULPerEndpoint.addItem (_createWarning ("The endpoint uses the non-standard transport profile '" +
                                                            aEndpoint.getTransportProfile () +
                                                            "'."));

                  if (aEndpoint.getServiceActivationDateTime () != null)
                  {
                    if (aEndpoint.getServiceActivationDateTime ().isAfter (aNowDT))
                      aULPerEndpoint.addItem (_createWarning ("The endpoint is not yet active."),
                                              div ("It will be active from " +
                                                   PDTToString.getAsString (aEndpoint.getServiceActivationDateTime (), aDisplayLocale) +
                                                   "."));
                  }

                  if (aEndpoint.getServiceExpirationDateTime () != null)
                  {
                    if (aEndpoint.getServiceExpirationDateTime ().isBefore (aNowDT))
                      aULPerEndpoint.addItem (_createError ("The endpoint is no longer active."),
                                              div ("It was valid until " +
                                                   PDTToString.getAsString (aEndpoint.getServiceExpirationDateTime (), aDisplayLocale) +
                                                   "."));
                    else
                      if (aEndpoint.getServiceExpirationDateTime ().isBefore (aNowPlusDT))
                        aULPerEndpoint.addItem (_createWarning ("The endpoint will be inactive soon."),
                                                div ("It is only valid until " +
                                                     PDTToString.getAsString (aEndpoint.getServiceExpirationDateTime (), aDisplayLocale) +
                                                     "."));
                  }

                  X509Certificate aX509Cert = null;
                  try
                  {
                    aX509Cert = CertificateHelper.convertStringToCertficate (aEndpoint.getCertificate ());
                  }
                  catch (final CertificateException ex)
                  {
                    // Ignore
                  }
                  if (aX509Cert == null)
                    aULPerEndpoint.addItem (_createError ("The X.509 certificate configured at the endpoint is invalid and could not be interpreted as a certificate."));
                  else
                  {
                    final LocalDateTime aNotBefore = PDTFactory.createLocalDateTime (aX509Cert.getNotBefore ());
                    if (aNowDT.isBefore (aNotBefore))
                      aULPerEndpoint.addItem (_createError ("The endpoint certificate is not yet active."),
                                              div ("It will be valid from " + PDTToString.getAsString (aNotBefore, aDisplayLocale) + "."));

                    final LocalDateTime aNotAfter = PDTFactory.createLocalDateTime (aX509Cert.getNotAfter ());
                    if (aNowDT.isAfter (aNotAfter))
                      aULPerEndpoint.addItem (_createError ("The endpoint certificate is already expired."),
                                              div ("It was valid until " + PDTToString.getAsString (aNotAfter, aDisplayLocale) + "."));
                    else
                      if (aNowPlusDT.isAfter (aNotAfter))
                        aULPerEndpoint.addItem (_createWarning ("The endpoint certificate will expire soon."),
                                                div ("It is only valid until " +
                                                     PDTToString.getAsString (aNotAfter, aDisplayLocale) +
                                                     "."));
                  }

                  // Show per endpoint errors
                  if (aULPerEndpoint.hasChildren ())
                    aULPerProcess.addItem (div ("Transport profile ").addChild (code (aEndpoint.getTransportProfile ())), aULPerEndpoint);
                }
                // Show per process errors
                if (aULPerProcess.hasChildren ())
                  aULPerDocType.addItem (div ("Process ").addChild (code (aProcess.getProcessIdentifier ()
                                                                                  .getURIEncoded ()).addClass (CUICoreCSS.CSS_CLASS_NOWRAP)),
                                         aULPerProcess);
              }
              // Show per document type errors
              if (aULPerDocType.hasChildren ())
                aULPerSG.addItem (div ("Document type ").addChild (code (aServiceInfo.getDocumentTypeIdentifier ()
                                                                                     .getURIEncoded ()).addClass (CUICoreCSS.CSS_CLASS_NOWRAP)),
                                  aULPerDocType);
            }
          }

          // Show per service group errors
          if (aULPerSG.hasChildren ())
            aOL.addItem (div ("Service group ").addChild (code (aServiceGroup.getParticpantIdentifier ().getURIEncoded ())), aULPerSG);
        }
      }
    }

    // Show results
    if (aOL.hasChildren ())
    {
      aNodeList.addChild (warn ("The following list of tasks and problems were identified:"));
      aNodeList.addChild (aOL);
    }
    else
      aNodeList.addChild (success ("Great job, no tasks or problems identified!"));
  }
}
