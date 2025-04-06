import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './App.css';

const CURRENCY_EUR = 'EUR';
const CURRENCY_USD = 'USD';

const NOTIFICATION_URL_TYPE1 = 'https://httpbin.org/get?source=type1_payment'; // Example endpoint that will succeed
const NOTIFICATION_URL_TYPE2 = 'https://httpbin.org/status/404?source=type2_payment';
// You can use services like webhook.site or Beeceptor to create temporary endpoints for testing

function PaymentRow({ payment, onCancel }) {
    const isCancelDisabled = (payment.cancelationFee !== null && payment.cancelationFee !== undefined) ||
                             (payment.cancelation !== null && payment.cancelation !== undefined);

    const formatCurrency = (amount, currencyCode) => {
        const numericAmount = parseFloat(amount);
        if (isNaN(numericAmount)) return '-';
        const symbol = currencyCode === CURRENCY_EUR ? '€' : '$';
        return `${symbol}${numericAmount.toFixed(2)}`;
    };

    const formatCancelationFee = (fee) => {
        const feeValue = fee;
        if (feeValue === null || feeValue === undefined) return '-';
        const numericFee = parseFloat(feeValue);
        if (isNaN(numericFee)) return '-';
        return `€${numericFee.toFixed(2)}`;
    }

    return (
        <tr key={payment.id}>
            <td className="col-id" title={payment.id}>
                {payment.id || '-'}
            </td>
            <td className="col-amount">
                {formatCurrency(payment.amount, payment.currency)}
            </td>
            <td className="col-fee">
                {formatCancelationFee(payment.cancelationFee ?? payment.cancelation)}
            </td>
            <td className="col-actions">
                <button
                    className="btn btn-danger"
                    onClick={() => onCancel(payment.id)}
                    disabled={isCancelDisabled}
                >
                    Cancel
                </button>
            </td>
        </tr>
    );
}


function App() {
    const [payments, setPayments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [minAmount, setMinAmount] = useState('');
    const [maxAmount, setMaxAmount] = useState('');

    const [searchByIdInput, setSearchByIdInput] = useState('');
    const [searchedPaymentResult, setSearchedPaymentResult] = useState(null);
    const [searchByIdLoading, setSearchByIdLoading] = useState(false);
    const [searchByIdError, setSearchByIdError] = useState(null);


    const initialNewPaymentState = {
        amount: '',
        currency: CURRENCY_EUR,
        debtorIban: '',
        creditorIban: '',
        details: '',
        bicCode: '',
        paymentType: 'TYPE1',
    };
    const [newPayment, setNewPayment] = useState(initialNewPaymentState);


    const fetchPayments = async () => {
        setLoading(true);
        try {
            const response = await axios.get('/payments');
            setPayments(response.data);
            setError(null);
        } catch (err) {
            setError('Error fetching payments: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchPayments();
    }, []);

    const filteredPayments = payments.filter(payment => {
        const paymentAmount = payment.amount ? parseFloat(payment.amount) : NaN;
        if (isNaN(paymentAmount)) return false;
        const min = minAmount ? parseFloat(minAmount) : 0;
        const max = maxAmount ? parseFloat(maxAmount) : Infinity;
        return paymentAmount >= min && paymentAmount <= max;
    });

    const handleCancelPayment = async (id) => {
        const paymentToCancel = payments.find(p => p.id === id) || (searchedPaymentResult?.id === id ? searchedPaymentResult : null);
        if (!paymentToCancel) {
            toast.error("Could not find payment details locally to send cancel request.");
            return;
        }
        const requestBody = {
            id: paymentToCancel.id,
            amount: String(paymentToCancel.amount),
            currency: paymentToCancel.currency,
            debtorIban: paymentToCancel.debtorIban,
            creditorIban: paymentToCancel.creditorIban,
            details: paymentToCancel.details || null,
            bicCode: paymentToCancel.bicCode || null,
            type: paymentToCancel.type,
        };
        try {
            const config = { headers: { 'Content-Type': 'application/json' } };
            await axios.put(`/payments/${id}`, requestBody, config);
            toast.success(`Payment ${id.substring(0, 8)}... successfully marked for cancelation.`);
            fetchPayments();
            if (searchedPaymentResult && searchedPaymentResult.id === id) {
                handleFindPaymentById(id);
            }
        } catch (err) {
            console.error("Cancelation failed:", err.response || err);
            const errorData = err.response?.data;
            let errorMsg = err.message;
            if (errorData) { /* ... error message extraction ... */ }
            toast.error(`Cancelation failed: ${errorMsg}`);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setNewPayment(prev => ({ ...prev, [name]: value }));
    };

    const handleTypeChange = (e) => {
        const type = e.target.value;
        let newCurrency = newPayment.currency;
        if (type === 'TYPE1') newCurrency = CURRENCY_EUR;
        else if (type === 'TYPE2') newCurrency = CURRENCY_USD;
        setNewPayment(prev => ({
            ...prev,
            paymentType: type,
            currency: newCurrency,
            details: (type === 'TYPE1') ? prev.details : '',
            bicCode: (type === 'TYPE3') ? prev.bicCode : ''
        }));
    };


    const sendNotification = async (paymentTypeNumeric, paymentId) => {
        let url;
        if (paymentTypeNumeric === 1) {
            url = NOTIFICATION_URL_TYPE1;
        } else if (paymentTypeNumeric === 2) {
            url = NOTIFICATION_URL_TYPE2;
        } else {
            return;
        }

        const typeString = `TYPE${paymentTypeNumeric}`;
        const idString = paymentId ? paymentId.substring(0, 8) + '...' : 'N/A';

        console.log(`[Notification] Attempting for ${typeString} (ID: ${idString}) -> ${url}`);

        try {
            const response = await axios.get(url, {
                timeout: 10000
            });

            console.log(`[Notification] Accepted: ${response.status} for ${typeString} (ID: ${idString})`);

        } catch (error) {
            if (error.response) {
                console.error(`[Notification] Declined: ${error.response.status} for ${typeString} (ID: ${idString})`);
            } else if (error.request) {
                 console.error(`[Notification] Failed (No Response) for ${typeString} (ID: ${idString}):`, error.message);
            } else {
                console.error(`[Notification] Failed (Request Setup Error) for ${typeString} (ID: ${idString}):`, error.message);
            }
        }
    };


    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validateForm()) return;

        let numericPaymentType;
        if (newPayment.paymentType === 'TYPE1') numericPaymentType = 1;
        else if (newPayment.paymentType === 'TYPE2') numericPaymentType = 2;
        else if (newPayment.paymentType === 'TYPE3') numericPaymentType = 3;
        else {
            toast.error("Invalid payment type.");
            return;
        }

        const paymentData = {
            amount: newPayment.amount,
            currency: newPayment.currency,
            debtorIban: newPayment.debtorIban,
            creditorIban: newPayment.creditorIban,
            details: newPayment.details || null,
            bicCode: newPayment.bicCode || null,
            type: numericPaymentType,
        };

        try {
            const config = { headers: { 'Content-Type': 'application/json' } };
            const response = await axios.post('/payments', paymentData, config);
            const createdPayment = response.data;

            toast.success(`Payment ${createdPayment?.id ? createdPayment.id.substring(0,8)+'...' : ''} created successfully`);

            if (numericPaymentType === 1 || numericPaymentType === 2) {
                sendNotification(numericPaymentType, createdPayment?.id);
            }

            setNewPayment(initialNewPaymentState);
            fetchPayments();

        } catch (err) {
            console.error("Payment creation error:", err.response || err);
            const errorData = err.response?.data;
            let errorMsg = err.message;
            if (errorData) { /* ... error message extraction ... */ }
            toast.error(`Failed to create payment: ${errorMsg}`);
        }
    };

    const validateForm = () => {
        if (!newPayment.amount || !newPayment.debtorIban || !newPayment.creditorIban) {
            toast.error('Amount, Debtor IBAN, and Creditor IBAN are required');
            return false;
        }
        if (isNaN(parseFloat(newPayment.amount)) || parseFloat(newPayment.amount) <= 0) {
            toast.error('Please enter a valid positive amount');
            return false;
        }
        const ibanRegex = /^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$/;
        if (!ibanRegex.test(newPayment.debtorIban) || !ibanRegex.test(newPayment.creditorIban)) {
            toast.error('Invalid IBAN format.');
            return false;
        }
        if (newPayment.paymentType === 'TYPE1') {
            if (!newPayment.details) { toast.error('Details are required for TYPE1'); return false; }
            if (newPayment.currency !== CURRENCY_EUR) { toast.error('TYPE1 must use EUR'); return false; }
        } else if (newPayment.paymentType === 'TYPE2') {
            if (newPayment.currency !== CURRENCY_USD) { toast.error('TYPE2 must use USD'); return false; }
        } else if (newPayment.paymentType === 'TYPE3') {
            if (!newPayment.bicCode) { toast.error('BIC code is required for TYPE3'); return false; }
            const bicRegex = /^[A-Z0-9]{8}([A-Z0-9]{3})?$/;
            if (!bicRegex.test(newPayment.bicCode)) { toast.error('Invalid BIC Code format'); return false; }
        }
        return true;
    };

    const handleFindPaymentById = async (idToSearch = null) => {
         const targetId = idToSearch || searchByIdInput;
        if (!targetId) {
            toast.info("Please enter a Payment ID to search.");
            return;
        }
        const uuidRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
        if (!uuidRegex.test(targetId)) {
            toast.error("Invalid ID format. Please enter a valid UUID.");
            setSearchByIdError("Invalid ID format.");
            setSearchedPaymentResult(null);
            return;
        }
        setSearchByIdLoading(true);
        setSearchByIdError(null);
        setSearchedPaymentResult(null);
        try {
            const response = await axios.get(`/payments/${targetId}`);
            setSearchedPaymentResult(response.data);
        } catch (err) {
            console.error("Error finding payment by ID:", err.response || err);
            if (err.response?.status === 404) {
                setSearchByIdError(`Payment with ID ${targetId.substring(0, 8)}... not found.`);
            } else {
                const errorData = err.response?.data;
                let errorMsg = err.message;
                if (errorData) { /* ... error message extraction ... */ }
                setSearchByIdError(`Error finding payment: ${errorMsg}`);
            }
            setSearchedPaymentResult(null);
        } finally {
            setSearchByIdLoading(false);
        }
    };

    const clearSingleSearchResult = () => {
        setSearchByIdInput('');
        setSearchedPaymentResult(null);
        setSearchByIdError(null);
        setSearchByIdLoading(false);
    };

    const getTableBodyContent = () => {
         if (searchByIdLoading) {
            return <tr><td colSpan="4">Loading payment details...</td></tr>;
        }
        if (searchByIdError) {
            return <tr><td colSpan="4" className="error">{searchByIdError}</td></tr>;
        }
        if (searchedPaymentResult) {
            return <PaymentRow payment={searchedPaymentResult} onCancel={handleCancelPayment} />;
        }
        if (loading) {
             return <tr><td colSpan="4">Loading payments...</td></tr>;
        }
        if (error) {
             return <tr><td colSpan="4" className="error">{error}</td></tr>;
        }
        if (filteredPayments.length > 0) {
            return filteredPayments.map(payment => (
                <PaymentRow key={payment.id} payment={payment} onCancel={handleCancelPayment} />
            ));
        }
        return <tr><td colSpan="4">No active payments found matching the criteria.</td></tr>;
    };


    return (
        <div className="container">
            <ToastContainer position="top-right" autoClose={3000} hideProgressBar />

            <h1>Payment Management System</h1>

            <div className="card">

                 <h2>Create New Payment</h2>
                <form onSubmit={handleSubmit}>
                     <div className="form-group">
                        <label>Payment Type:</label>
                        <select name="paymentType" value={newPayment.paymentType} onChange={handleTypeChange}>
                            <option value="TYPE1">Type 1 (EUR only, details required)</option>
                            <option value="TYPE2">Type 2 (USD only, optional details)</option>
                            <option value="TYPE3">Type 3 (EUR/USD, BIC required)</option>
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Amount:</label>
                        <input type="number" name="amount" value={newPayment.amount} onChange={handleInputChange} step="0.01" min="0.01" placeholder="e.g., 123.45" required/>
                    </div>
                    <div className="form-group">
                        <label>Currency:</label>
                        <select name="currency" value={newPayment.currency} onChange={handleInputChange} disabled={newPayment.paymentType === 'TYPE1' || newPayment.paymentType === 'TYPE2'} required>
                            <option value={CURRENCY_EUR}>EUR (€)</option>
                            <option value={CURRENCY_USD}>USD ($)</option>
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Debtor IBAN:</label>
                        <input type="text" name="debtorIban" value={newPayment.debtorIban} onChange={handleInputChange} placeholder="e.g., LT12 1234 1234 1234 1234" required/>
                    </div>
                    <div className="form-group">
                        <label>Creditor IBAN:</label>
                        <input type="text" name="creditorIban" value={newPayment.creditorIban} onChange={handleInputChange} placeholder="e.g., LT12 1234 1234 1234 1234" required/>
                    </div>
                    {(newPayment.paymentType === 'TYPE1' || newPayment.paymentType === 'TYPE2') && (
                    <div className="form-group">
                        <label>Details:</label>
                        <input type="text" name="details" value={newPayment.details} onChange={handleInputChange} required={newPayment.paymentType === 'TYPE1'} placeholder={newPayment.paymentType === 'TYPE1' ? "Required" : ""}/>
                    </div>
                    )}
                    {newPayment.paymentType === 'TYPE3' && (
                    <div className="form-group">
                        <label>BIC Code:</label>
                        <input type="text" name="bicCode" value={newPayment.bicCode} onChange={handleInputChange} placeholder="e.g., DEUTDEFF" required/>
                    </div>
                    )}
                    <button type="submit" className="btn btn-primary">Create Payment</button>
                </form>
            </div>

            <div className="card">
                 <h2>Filter Active Payments</h2>
                <div className="filters">
                    <div className="filter-group">
                        <label>Min Amount:</label>
                        <input type="number" value={minAmount} onChange={(e) => setMinAmount(e.target.value)} min="0" step="0.01" placeholder="e.g., 10.00"/>
                    </div>
                    <div className="filter-group">
                        <label>Max Amount:</label>
                        <input type="number" value={maxAmount} onChange={(e) => setMaxAmount(e.target.value)} min="0" step="0.01" placeholder="e.g., 500.00"/>
                    </div>
                    <button className="btn btn-secondary" onClick={() => { setMinAmount(''); setMaxAmount(''); }}>
                        Clear Amount Filters
                    </button>
                </div>
            </div>

            <div className="card">
                 <h2>Find Payment by ID</h2>
                <div className="single-search-form filters">
                    <div className="filter-group">
                        <label>Payment ID:</label>
                        <input
                            type="text"
                            value={searchByIdInput}
                            onChange={(e) => setSearchByIdInput(e.target.value)}
                            placeholder="Enter exact Payment ID (UUID)"
                            className="search-id-input"
                        />
                    </div>
                    <button
                        className="btn btn-primary"
                        onClick={() => handleFindPaymentById()}
                        disabled={!searchByIdInput || searchByIdLoading}
                    >
                        {searchByIdLoading ? 'Searching...' : 'Find Payment'}
                    </button>
                    {(searchByIdInput || searchedPaymentResult || searchByIdError) && (
                        <button
                            className="btn btn-secondary"
                            onClick={clearSingleSearchResult}
                        >
                            Clear ID Search / Show List
                        </button>
                    )}
                </div>
            </div>

            <div className="card">
                 <h2>{searchedPaymentResult ? 'Payment Details' : 'Payment Transactions'}</h2>
                <div className="table-responsive">
                    <table>
                        <thead>
                            <tr>
                                <th className="col-id">ID</th>
                                <th className="col-amount">Amount</th>
                                <th className="col-fee">Cancelation Fee</th>
                                <th className="col-actions">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {getTableBodyContent()}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

export default App;