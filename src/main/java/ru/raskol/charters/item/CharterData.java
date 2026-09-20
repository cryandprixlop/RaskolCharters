package ru.raskol.charters.item;

/** Данные королевской грамоты (хранятся в PDC предмета). */
public final class CharterData {

    public double amount;      // сумма за одну выплату
    public String frequency;   // hour | day | week
    public long issuedAt;      // ms выдачи
    public long expiresAt;     // ms истечения срока
    public long nextPayAt;     // ms следующей выплаты
    public String nation;      // нация-плательщик
    public String issuer;      // имя короля, выдавшего грамоту

    public boolean isExpired(long now) {
        return now >= expiresAt;
    }
}
