package webhook.processor;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.ExecutionException;

@Slf4j
@Getter
@Setter
@Service
public class TelegramBotServiceImpl extends TelegramLongPollingBot {

    private final int RECONNECT_PAUSE = 10000;

    @Value("${bot.name}")
    private String username;
    @Value("${bot.token}")
    private String token;
    @Value("${bot.chatid}")
    private String chatId;

    @PostConstruct
    private void post() {
        TelegramBotServiceImpl telegramBot = new TelegramBotServiceImpl();
        telegramBot.setUsername(username);
        telegramBot.setToken(token);
        telegramBot.setChatId(chatId);
        try {
            telegramBot.botConnect();
        } catch (TelegramApiException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasChannelPost() && update.getChannelPost().hasVideoNote()) {
            if (update.getChannelPost().getForwardFrom() != null ||
            update.getChannelPost().getForwardFromChat() != null ||
            update.getChannelPost().getForwardFromMessageId() != null) {
                update.getChannelPost().setForwardFrom(null);
                update.getChannelPost().setForwardFromChat(null);
                update.getChannelPost().setForwardFromMessageId(null);
                update.getChannelPost().setForwardDate(null);
                update.getChannelPost().setForwardSignature(null);
                update.getChannelPost().setForwardSenderName(null);
            }

        } else {
            deleteMessage(update.getChannelPost().getChatId(), update.getChannelPost().getMessageId());
        }
    }

    private void deleteMessage(Long chatId, Integer messageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage(chatId.toString(), messageId);
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void botConnect() throws TelegramApiException, ExecutionException, InterruptedException {
        log.info("botConnect start");
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            telegramBotsApi.registerBot(this);
            log.info("TelegramAPI started. Look for messages");
        } catch (TelegramApiException e) {
            log.error("Cant Connect. Pause " + RECONNECT_PAUSE / 1000 + "sec and try again. Error: " + e.getMessage());
            try {
                Thread.sleep(RECONNECT_PAUSE);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
                return;
            }
            botConnect();
        }

    }
}