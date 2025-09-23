package com.banula.navigationservice.service;

import com.banula.openlib.ocpi.model.ClientInfo;

public interface NSPNotificationService {
    void broadcastHubClientInfoUpdate(ClientInfo clientInfo);
}
