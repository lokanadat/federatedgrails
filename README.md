# FederatedGrails - 0.1

Allows Grails applications to easily integrate to federated authentication sources particuarly those served by Shibboleth service providers.

Utilizes Apache Shiro as an underlying mechanism so serves managing access control requirements equally as well. I recommend you read the Shiro documentation before continuing to get a feel for how this plugin operates - http://www.grails.org/plugin/shiro

Concepts
--------
* Subject - Security specific view of an entity capable of being authenticated to an application. It can be a human being, a third-party process, a server etc. Also referred to as ‘user’.
* Principal - A subjects uniquely identifying attribute. This is generally mapped to the federation attribute eduPersonTargetedID. For non federated applications this is commonly referred to as a ‘username’
* Credentials - Data used to verify identity at session establishment. For integrators this is the associated SAML assertion and is represented by a unique internal sessionID. For non federated applications this is usually a ‘password’
* Attributes - A subjects identifying attributes. Names, email, entitlements etc.For non federated applications these need to manually entered. For federated applications they are in many cases automatically supplied.

Demonstration
-------------
To get started a *very* basic sample app is provided at test/dummy.
 
    grails run-app

to try it out.

Installation
------------
Prerequisite: Your Shibboleth SP is setup and working correctly (this doesn't apply if you simply wish to enable development mode on your local development environment).

This version isn't published to rails plugins given early release status.

Clone the repository to your local disk and in your BuildConfig.groovy file add:

    grails.plugin.location.'federatedgrails'="your path"  

Once this is done execute the supplied script to integrate

    grails integrate-federation Package Subject

* Package should be your applications package structure eg au.edu.uniname
* Subject should be the name of your subject class you can change this to say User if this better suits your application.

All controllers except those supplied by the plugin will now be authenticated to the federation. Update grails-app/conf/SecurityFilters.groovy to make this more restrictive.

Configuration
-------------
All configuration is managed in:

    grails-app/conf/FederationConfig.groovy

* automatelogin - When the user needs to login they will be forwarded directly to the Shibboleth configured endpoint and not shown the login view.
* federationactive - The application is being protected by a shibboleth instance
* developmentactive - The application is in development mode and accounts may be specified on the login page
* autoprovision - New users to the system will be automatically created
* subject - The name of your subject class, generally Subject but maybe User is you changed during installation.
* ssoendpoint - The endpoint within you Shibboleth SP configuration that will initiate a session
* attributes - To use apache environment variables or HTTP headers to retrieve attributes. If you're making use of mod_jk to interface to your Tomcat instance from Apache this should be true.
* mapping.XYZ - The set of attributes you expect to be delivered to your SP by remote IdP and available in your app

Advanced Integration
--------------------
To allow deployers to customize how their Subject objects are provisioned you can execute

    grails advanced-federation

This will provide you a copy of the AuthController and Federated/Database Realms. Generally the best place to start making extensions is within the provisioning and update routines located within FederatedRealm.groovy

Contributing
------------
All contributions welcome. Simply fork and send a pull request when you have something new or log an issue.

For code changes please ensure you supply working tests and documentation.

Requests for additional information to go along with this documentation are also welcomed.

Credits
-------
FederatedGrails was written by Bradley Beddoes on behalf of the Australian Access Federation.

