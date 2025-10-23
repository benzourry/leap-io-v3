// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title DataRegistry
 * @dev A general-purpose registry contract for storing, updating, and revoking key-value data pairs.
 * Each entry has an ID (uint256) and an associated string value.
 * Only the owner can modify data.
 */
contract DataRegistry {
    struct DataItem {
        string data;
        bool valid;
    }

    // Mapping from dataId → DataItem
    mapping(uint256 => DataItem) private dataStore;

    address public owner;

    // Events
    event DataAdded(uint256 indexed dataId, string data);
    event BatchDataAdded(uint256 startIndex, uint256 endIndex, uint256 count);
    event DataUpdated(uint256 indexed dataId, string oldData, string newData);
    event BatchDataUpdated(uint256 startIndex, uint256 endIndex, uint256 count);
    event DataRevoked(uint256 indexed dataId);

    // --- Access Control ---
    modifier onlyOwner() {
        require(msg.sender == owner, "Not authorized");
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    // --- Write Functions ---

    /**
     * @dev Add a new data entry.
     */
    function addData(uint256 dataId, string calldata data) external onlyOwner {
        require(!dataStore[dataId].valid, "Already exists");
        dataStore[dataId] = DataItem({ data: data, valid: true });
        emit DataAdded(dataId, data);
    }

    /**
     * @dev Add multiple data entries in a single transaction (batch add).
     * @notice Use with caution — large batches can consume high gas.
     */
    function addDataBatch(uint256[] calldata dataIds, string[] calldata dataList)
        external
        onlyOwner
    {
        uint256 length = dataIds.length;
        require(length == dataList.length, "Mismatched array lengths");
        require(length > 0, "Empty batch");
        require(length <= 500, "Batch too large");

        for (uint256 i = 0; i < length; i++) {
            uint256 dataId = dataIds[i];
            require(!dataStore[dataId].valid, "Already exists");
            dataStore[dataId] = DataItem({ data: dataList[i], valid: true });
        }

        emit BatchDataAdded(dataIds[0], dataIds[length - 1], length);
    }

    /**
     * @dev Update existing data entry.
     */
    function updateData(uint256 dataId, string calldata newData)
        external
        onlyOwner
    {
        require(dataStore[dataId].valid, "Data not found or invalid");
        string memory oldData = dataStore[dataId].data;
        dataStore[dataId].data = newData;
        emit DataUpdated(dataId, oldData, newData);
    }

    /**
     * @dev Batch update existing data entries.
     */
    function updateDataBatch(uint256[] calldata dataIds, string[] calldata newDataList)
        external
        onlyOwner
    {
        uint256 length = dataIds.length;
        require(length == newDataList.length, "Mismatched array lengths");
        require(length > 0, "Empty batch");
        require(length <= 500, "Batch too large");

        for (uint256 i = 0; i < length; i++) {
            uint256 dataId = dataIds[i];
            require(dataStore[dataId].valid, "Invalid dataId");
            dataStore[dataId].data = newDataList[i];
        }

        emit BatchDataUpdated(dataIds[0], dataIds[length - 1], length);
    }

    /**
     * @dev Revoke (invalidate) a data entry.
     */
    function revokeData(uint256 dataId) external onlyOwner {
        require(dataStore[dataId].valid, "Not found or already revoked");
        dataStore[dataId].valid = false;
        emit DataRevoked(dataId);
    }

    // --- Read Functions ---

    /**
     * @dev Returns the stored data for a given ID.
     */
    function getData(uint256 dataId) external view returns (string memory) {
        require(dataStore[dataId].valid, "Data not found or invalid");
        return dataStore[dataId].data;
    }

    /**
     * @dev Returns whether the given ID is valid (active).
     */
    function isValid(uint256 dataId) external view returns (bool) {
        return dataStore[dataId].valid;
    }

    /**
     * @dev Get contract owner.
     */
    function getOwner() external view returns (address) {
        return owner;
    }
}
