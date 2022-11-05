package com.thcplusplus.t3d;

import android.util.Log;

public class GamePlay {
    public static final byte DIMENSION = 4, FACES = 4, NUMBER_OF_CELLS = 64;
    private int mTurn = 1;
    private int[] mPlayerPoints;
    private byte[][][] mGameGrid;
    public byte mCellFilled = 0;

    public GamePlay()
    {
        mGameGrid = new byte[FACES][DIMENSION][DIMENSION];
        mPlayerPoints = new int[2];
    }

    public int getTurn() {
        return mTurn;
    }

    public String getBillboardText()
    {
        return "Player X: " + Integer.toString(mPlayerPoints[0]) + "                " + "Player O: " + Integer.toString(mPlayerPoints[1]);
    }

    public Boolean setNewMove(final byte face,final byte row,final byte column){
        if(mGameGrid[face][row][column] != 0)
            return false;
        mGameGrid[face][row][column] = (byte) mTurn;
        mTurn = mTurn == 1 ? 2 : 1 ;
        mCellFilled++;
        return true;
    }

    public byte declareWinner() {
        if(mPlayerPoints[0] > mPlayerPoints[1])
            return 1;//player 1 won
        if(mPlayerPoints[1] > mPlayerPoints[0])
            return 2; // player 2 won
        return 0; // draw
    }

    public Boolean checkGameGrid(final byte face,final byte row,final byte column)
    {
        byte xCount = 0, yCount = 0, mainDiagCount = 0, sideDiagCount = 0;
        final byte player = mGameGrid[face][row][column];
        for(int i = 0; i < DIMENSION; i++){
            if(mGameGrid[face][row][i] == player)
                xCount++;
            if(mGameGrid[face][i][column] == player)
                yCount++;
            if(row == column && mGameGrid[face][i][i] == player)
                mainDiagCount++;
            if(row + column + 1 == DIMENSION && mGameGrid[face][i][DIMENSION - i - 1] == player)
                sideDiagCount++;

        }
        
        byte xCompleted = (byte)(xCount / 4) , yCompleted = (byte) (yCount / 4),
                mainDiagCompleted = (byte) (mainDiagCount / 4), sideDiagCompleted = (byte) (sideDiagCount / 4);
        if(xCompleted == 1 || yCompleted == 1 || mainDiagCompleted == 1 || sideDiagCompleted == 1){
            mPlayerPoints[player - 1] += xCompleted + yCompleted + mainDiagCompleted + sideDiagCompleted;
            Log.e("DEBUG","p1: " + Integer.toString(mPlayerPoints[0]) + "\tp2: " + Integer.toString(mPlayerPoints[1]));
            return true;
        }
        return false;
    }

    public boolean isPlayerInTurn(String playerRoleInRoom) {
        if( (playerRoleInRoom.equals(JoinRoomActivity.HOST_TAG) && mTurn == 1) || (playerRoleInRoom.equals(JoinRoomActivity.GUEST_TAG) && mTurn == 2))
            return true;
        return false;
    }
}
